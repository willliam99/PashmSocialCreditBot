FROM eclipse-temurin:23

RUN mkdir /opt/app

COPY build/libs/PashmSocialCreditBot-2.1.6.jar /opt/app

EXPOSE 10000

CMD ["java", "-jar", "/opt/app/PashmSocialCreditBot-2.1.6.jar"]