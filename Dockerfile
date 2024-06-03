
FROM mozilla/sbt as builder
COPY . /lambda/src/
WORKDIR /lambda/src/
RUN sbt assembly

FROM public.ecr.aws/lambda/java:11
COPY --from=builder /lambda/src/target/scala-2.13/mobile-notifications-content.jar ${LAMBDA_TASK_ROOT}/lib/
CMD ["com.gu.mobile.content.notifications.ContentLambda::handler"]
