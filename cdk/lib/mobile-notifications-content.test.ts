import { App } from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { MobileNotificationsContent } from './mobile-notifications-content';

describe('The MobileNotificationsContent stack', () => {
	it('matches the snapshot', () => {
		const app = new App();
		const stack = new MobileNotificationsContent(
			app,
			'MobileNotificationsContent',
			{
				stack: 'content-api-mobile-notifications',
				stage: 'TEST',
				crossAccountSsmRole:
					'arn:aws:iam::201359054765:role/mobile-content-notifications-lambda-cross-account-ssm-TEST',
				crossAccountDynamoRole:
					'arn:aws:iam::201359054765:role/mobile-content-notifications-lambda-cross-account-dynamo-TEST',
				kinesisStreamArn:
					'arn:aws:kinesis:eu-west-1:308506855511:stream/content-api-firehose-v2-TEST',
				snsAlarmTopicArn:
					'arn:aws:sns:eu-west-1:201359054765:mobile-server-side',
			},
		);
		const template = Template.fromStack(stack);
		expect(template.toJSON()).toMatchSnapshot();
	});
});
