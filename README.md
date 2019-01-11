# mobile content notifications lambda

This is a lambda that replaces the [Mobile notifications content]([https://github.com/guardian/mobile-notifications-content]) service. Instead of running as a standalone service on it's own ELB, is a [https://aws.amazon.com/lambda](lambda) that runs in the CAPI aws account

The lambda consumes the CAPI [Firehose](http://docs.aws.amazon.com/firehose/latest/dev/what-is-this-service.html) and sends as notification request for a new pieces of content to the [Mobile notifications service](https://github.com/guardian/mobile-notifications) 

## Deployment

Deploy the latest build of **Mobile::mobile-notifications-content** in [RiffRaff](https://riffraff.gutools.co.uk)  this redeploys the Lambda in the CAPI account. If you need to redeploy the whole stack you'll need to get access to the CAPI account via [janus](https://janus.gutools.co.uk) go to Cloudformation console and rebuild or create the stack using the cloudformation script in this repo. 
 
## Cross account resources

The lambda gets it's configuration and does some simple content tracking using AWS resources in the mobile account, using cross account roles. These resources are created by this [Cloudformation](The lambda gets it's configuration and does some simple content tracking using AWS resources in the mobile account, using cross account roles. These resources are created by this [Cloudformation]()
 here. 