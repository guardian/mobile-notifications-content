import type { GuStackProps } from '@guardian/cdk/lib/constructs/core';
import { GuStack } from '@guardian/cdk/lib/constructs/core';
import { type App, CfnParameter, Duration, Fn } from 'aws-cdk-lib';
import {
	Alarm,
	ComparisonOperator,
	MathExpression,
	TreatMissingData,
} from 'aws-cdk-lib/aws-cloudwatch';
import { SnsAction } from 'aws-cdk-lib/aws-cloudwatch-actions';
import { Repository } from 'aws-cdk-lib/aws-ecr';
import { PolicyStatement, Role, ServicePrincipal } from 'aws-cdk-lib/aws-iam';
import {
	DockerImageCode,
	DockerImageFunction,
	StartingPosition,
} from 'aws-cdk-lib/aws-lambda';
import { Topic } from 'aws-cdk-lib/aws-sns';

interface MobileNotificationsContentProps extends GuStackProps {
	crossAccountSsmRole: string;
	crossAccountDynamoRole: string;
	kinesisStreamArn: string;
	snsAlarmTopicArn: string;
}



export class MobileNotificationsContent extends GuStack {
	percentageAlarm(id: string, lamdba: DockerImageFunction, scope: MobileNotificationsContent) {
		const percentageErrorsContent = new MathExpression({
			expression: '100*m1/m2',
			usingMetrics: {
				m1: lamdba.metricErrors(),
				m2: lamdba.metricInvocations(),
			},
			label: `Error % of ${lamdba.functionName}`,
			period: Duration.minutes(10),
		});

		return new Alarm(scope, id, {
			alarmDescription: `High error percentage from ${lamdba.functionName} lambda in ${this.stage}`,
			comparisonOperator: ComparisonOperator.GREATER_THAN_THRESHOLD,
			evaluationPeriods: 1,
			threshold: 0,
			metric: percentageErrorsContent,
			treatMissingData: TreatMissingData.NOT_BREACHING,
		});
	}
	constructor(scope: App, id: string, props: MobileNotificationsContentProps) {
		super(scope, id, props);

		const appName = 'mobile-notifications-content';

		const contentImageRepositoryArn = Fn.importValue(
			'mobile-notifications-shared-resources-ContentLambdaContainerRepositoryArn',
		);

		const contentImageRepositoryName = Fn.importValue(
			'mobile-notifications-shared-resources-ContentLambdaContainerRepositoryUri',
		);

		const liveBlogsImageRepositoryArn = Fn.importValue(
			'mobile-notifications-shared-resources-LiveblogsLambdaContainerRepositoryArn',
		);

		const liveBlogsImageRepositoryName = Fn.importValue(
			'mobile-notifications-shared-resources-LiveblogsLambdaContainerRepositoryUri',
		);
		const buildId = new CfnParameter(this, 'BuildId', {
			type: 'String',
			default: 'dev',
			description: 'Tag to be used for the image URL, e.g. riff raff build id',
		}).value.toString();

		const executionRole = new Role(this, 'ExecutionRole', {
			assumedBy: new ServicePrincipal('lambda.amazonaws.com'),
			path: '/',
		});

		executionRole.addToPolicy(
			new PolicyStatement({
				actions: ['sts:AssumeRole'],
				resources: [props.crossAccountSsmRole],
			}),
		);

		executionRole.addToPolicy(
			new PolicyStatement({
				actions: ['sts:AssumeRole'],
				resources: [props.crossAccountDynamoRole],
			}),
		);

		executionRole.addToPolicy(
			new PolicyStatement({
				actions: [
					'logs:CreateLogGroup',
					'logs:CreateLogStream',
					'logs:PutLogEvents',
				],
				resources: ['*'],
			}),
		);

		executionRole.addToPolicy(
			new PolicyStatement({
				actions: ['lambda:InvokeFunction'],
				resources: ['*'],
			}),
		);

		executionRole.addToPolicy(
			new PolicyStatement({
				actions: [
					'iam:PassRole',
					'iam:GenerateCredentialReport',
					'iam:Get*',
					'iam:List*',
				],
				resources: ['*'],
			}),
		);

		executionRole.addToPolicy(
			new PolicyStatement({
				actions: [
					'Kinesis:DescribeStream',
					'Kinesis:GetRecords',
					'Kinesis:GetShardIterator',
					'Kinesis:ListStream',
				],
				resources: ['*'],
			}),
		);

		executionRole.addToPolicy(
			new PolicyStatement({
				actions: ['cloudwatch:PutMetricData'],
				resources: ['*'],
			}),
		);

		const snsTopicAction = new SnsAction(
			Topic.fromTopicArn(this, 'AlarmTopic', props.snsAlarmTopicArn),
		);
		const contentLambda = new DockerImageFunction(this, 'ContentLambda', {
			functionName: `${appName}-${this.stage}-v2`,
			code: DockerImageCode.fromEcr(
				Repository.fromRepositoryAttributes(this, `${appName}-ecr`, {
					repositoryArn: contentImageRepositoryArn,
					repositoryName: contentImageRepositoryName,
				}),
				{
					tagOrDigest: buildId,
				},
			),
			environment: {
				Stage: this.stage,
				Stack: this.stack,
				App: appName,
				CrossAccountSsmReadingRole: `${props.crossAccountSsmRole}`,
			},
			memorySize: 4096,
			description: `Lambda to send notification when new content is published`,
			role: executionRole,
			timeout: Duration.seconds(60),
		});

		contentLambda.addEventSourceMapping('ContentLambdaEventMapping', {
			eventSourceArn: `${props.kinesisStreamArn}`,
			startingPosition: StartingPosition.LATEST,
			enabled: true,
			bisectBatchOnError: true,
		});
		const contentLambdaAlarm = this.percentageAlarm(`${appName}-alarm`, contentLambda, this)

		contentLambdaAlarm.addAlarmAction(snsTopicAction);
		contentLambdaAlarm.addOkAction(snsTopicAction);

		const liveBlogsLambda = new DockerImageFunction(this, 'LiveblogsLambda', {
			functionName: `${appName}-liveblogs-${this.stage}-v2`,
			code: DockerImageCode.fromEcr(
				Repository.fromRepositoryAttributes(this, `${appName}-liveblogs-ecr`, {
					repositoryArn: liveBlogsImageRepositoryArn,
					repositoryName: liveBlogsImageRepositoryName,
				}),
				{
					tagOrDigest: buildId,
				},
			),
			environment: {
				Stage: this.stage,
				Stack: this.stack,
				App: appName,
				CrossAccountSsmReadingRole: `${props.crossAccountSsmRole}`,
			},
			memorySize: 4096,
			description: `Lambda that sends push notifications when new key events are published on a liveblo`,
			role: executionRole,
			timeout: Duration.seconds(60),
		});

		liveBlogsLambda.addEventSourceMapping('LiveBlogsLambdaEventMapping', {
			eventSourceArn: `${props.kinesisStreamArn}`,
			startingPosition: StartingPosition.LATEST,
			enabled: true,
			bisectBatchOnError: true,
		});

		const liveBlogsLambdaAlarm = this.percentageAlarm(`${appName}-liveblogs-alarm`,liveBlogsLambda, this)
		liveBlogsLambdaAlarm.addAlarmAction(snsTopicAction);
		liveBlogsLambdaAlarm.addOkAction(snsTopicAction);
	}
}
