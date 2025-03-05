import "source-map-support/register";
import { GuRoot } from "@guardian/cdk/lib/constructs/root";
import { MobileNotificationsContent } from "../lib/mobile-notifications-content";

const app = new GuRoot();
new MobileNotificationsContent(app, "MobileNotificationsContent-CODE", { stack: "content-api-mobile-notifications", stage: "CODE", env: { region: "eu-west-1" } });
new MobileNotificationsContent(app, "MobileNotificationsContent-PROD", { stack: "content-api-mobile-notifications", stage: "PROD", env: { region: "eu-west-1" } });
