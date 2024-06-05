
FROM public.ecr.aws/lambda/java:11
COPY mobile-notifications-content.jar ${LAMBDA_TASK_ROOT}/lib/
CMD ["com.gu.mobile.content.notifications.ContentLambda::handler"]
