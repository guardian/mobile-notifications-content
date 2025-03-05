import { App } from "aws-cdk-lib";
import { Template } from "aws-cdk-lib/assertions";
import { MobileNotificationsContent } from "./mobile-notifications-content";

describe("The MobileNotificationsContent stack", () => {
  it("matches the snapshot", () => {
    const app = new App();
    const stack = new MobileNotificationsContent(app, "MobileNotificationsContent", { stack: "content-api-mobile-notifications", stage: "TEST" });
    const template = Template.fromStack(stack);
    expect(template.toJSON()).toMatchSnapshot();
  });
});
