# mobile content notifications lambda

This is a lambda that replaces the [Mobile notifications content]([https://github.com/guardian/mobile-notifications-content]) service. Instead of running as a standalone service on it's own ELB, is a [https://aws.amazon.com/lambda](lambda) that runs in the CAPI aws account

The lambda consumes the CAPI [Firehose](http://docs.aws.amazon.com/firehose/latest/dev/what-is-this-service.html) and sends as notification request for a new pieces of content to the [Mobile notifications service](https://github.com/guardian/mobile-n10n) 

## Deployment

Deploy the latest build of **Mobile::mobile-notifications-content** in [RiffRaff](https://riffraff.gutools.co.uk)  this redeploys the Lambda in the CAPI account. If you need to redeploy the whole stack you'll need to get access to the CAPI account via [janus](https://janus.gutools.co.uk) go to Cloudformation console and rebuild or create the stack using the cloudformation script in this repo. 
 
## Cross account resources

The lambda gets it's configuration and does some simple content tracking using AWS resources in the mobile account, using cross account roles. These resources are created by this [Cloudformation](The lambda gets it's configuration and does some simple content tracking using AWS resources in the mobile account, using cross account roles. These resources are created by this [Cloudformation]()
 here.

## Testing on CODE

1) Go to [Composer in CODE](https://composer.code.dev-gutools.co.uk/)  (You may need to speak with a team adminstrator to get permissions to do this)
   - Publish a new article that has a Series tag applied, or
   - Publish a live blog key event entry
3) Monitor the [logs of the lambda](https://logs.gutools.co.uk/s/content-platforms/app/discover#/?_g=(filters:!(),refreshInterval:(pause:!t,value:0),time:(from:now-15m,to:now))&_a=(columns:!(),filters:!(('$state':(store:appState),meta:(alias:!n,disabled:!f,index:b0be43a0-59d7-11e8-a75a-b7af20e8f748,key:stage,negate:!f,params:(query:CODE),type:phrase),query:(match_phrase:(stage:CODE))),('$state':(store:appState),meta:(alias:!n,disabled:!f,index:b0be43a0-59d7-11e8-a75a-b7af20e8f748,key:app,negate:!f,params:(query:mobile-notifications-content),type:phrase),query:(match_phrase:(app:mobile-notifications-content)))),index:b0be43a0-59d7-11e8-a75a-b7af20e8f748,interval:auto,query:(language:kuery,query:''),sort:!(!('@timestamp',desc))))
4) Observe the [logs of the notification api](https://logs.gutools.co.uk/s/mobile/app/discover#/view/8cc67480-07a1-11ef-a06c-911a16951718?_g=(filters:!(),refreshInterval:(pause:!t,value:0),time:(from:now-2h,to:now))&_a=(columns:!(message),filters:!(('$state':(store:appState),meta:(alias:!n,disabled:!f,index:'26bd67e0-d55e-11e9-923e-49c5b785a0b2',key:app,negate:!f,params:(query:notification),type:phrase),query:(match_phrase:(app:notification))),('$state':(store:appState),meta:(alias:!n,disabled:!f,index:'26bd67e0-d55e-11e9-923e-49c5b785a0b2',key:stage,negate:!f,params:(query:CODE),type:phrase),query:(match_phrase:(stage:CODE)))),grid:(),hideChart:!f,index:'26bd67e0-d55e-11e9-923e-49c5b785a0b2',interval:auto,query:(language:kuery,query:'%22Notification%20was%20sent%22'),sort:!(!('@timestamp',desc))))
You should see an entry appear to validate that the publication event has been consumed by the lambda (e.g. `Notification was sent: ContentNotification(..)`).
