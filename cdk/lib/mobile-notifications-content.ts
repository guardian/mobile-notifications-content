import { join } from 'path';
import type { GuStackProps } from '@guardian/cdk/lib/constructs/core';
import { GuStack } from '@guardian/cdk/lib/constructs/core';
import { type App, CfnParameter, Duration, Fn } from 'aws-cdk-lib';
import { Repository } from 'aws-cdk-lib/aws-ecr';
import { PolicyStatement, Role, ServicePrincipal } from 'aws-cdk-lib/aws-iam';
import {
	DockerImageCode,
	DockerImageFunction,
	StartingPosition,
} from 'aws-cdk-lib/aws-lambda';
import { CfnInclude } from 'aws-cdk-lib/cloudformation-include';

interface MobileNotificationsContentProps extends GuStackProps {
	crossAccountSsmRole: string;
	crossAccountDynamoRole: string;
	kinesisStreamArn: string;
}
export class MobileNotificationsContent extends GuStack {
	constructor(scope: App, id: string, props: MobileNotificationsContentProps) {
		super(scope, id, props);
		const yamlTemplateFilePath = join(__dirname, '../..', 'cfn.yaml');
		new CfnInclude(this, 'YamlTemplate', {
			templateFile: yamlTemplateFilePath,
		});

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
	}
}
