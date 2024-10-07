import "source-map-support/register";
import { GuRoot } from "@guardian/cdk/lib/constructs/root";
import { MobileNotificationsContent } from "../lib/mobile-notifications-content";

const app = new GuRoot();
new MobileNotificationsContent(app, "MobileNotificationsContent-euwest-1-CODE", { stack: "content-api-mobile-notifications", stage: "CODE", env: { region: "eu-west-1" } });
new MobileNotificationsContent(app, "MobileNotificationsContent-euwest-1-PROD", { stack: "content-api-mobile-notifications", stage: "PROD", env: { region: "eu-west-1" } });
