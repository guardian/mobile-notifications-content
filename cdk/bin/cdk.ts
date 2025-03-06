import 'source-map-support/register';
import { GuRoot } from '@guardian/cdk/lib/constructs/root';
import { MobileNotificationsContent } from '../lib/mobile-notifications-content';

const app = new GuRoot();
new MobileNotificationsContent(app, 'MobileNotificationsContent-CODE', {
	stack: 'content-api-mobile-notifications',
	stage: 'CODE',
	crossAccountSsmRole:
		'arn:aws:iam::201359054765:role/mobile-content-notifications-lambda-cross-account-ssm-CODE',
	crossAccountDynamoRole:
		'arn:aws:iam::201359054765:role/mobile-content-notifications-lambda-cross-account-dynamo-CODE',
	kinesisStreamArn:
		'arn:aws:kinesis:eu-west-1:308506855511:stream/content-api-firehose-v2-CODE',
	env: { region: 'eu-west-1' },
});

new MobileNotificationsContent(app, 'MobileNotificationsContent-PROD', {
	stack: 'content-api-mobile-notifications',
	stage: 'PROD',
	crossAccountSsmRole:
		'arn:aws:iam::201359054765:role/mobile-content-notifications-lambda-cross-account-ssm-PROD',
	crossAccountDynamoRole:
		'arn:aws:iam::201359054765:role/mobile-content-notifications-lambda-cross-account-dynamo-PROD',
	kinesisStreamArn:
		'arn:aws:kinesis:eu-west-1:308506855511:stream/content-api-firehose-v2-PROD',
	env: { region: 'eu-west-1' },
});
