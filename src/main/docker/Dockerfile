FROM tomcat:10.1.8
LABEL mantainer="dudoleitor@dudoleitor.com"
ADD tiw-ria.war /usr/local/tomcat/webapps/ROOT.war
RUN chown 65210:65210 -R /usr/local/tomcat/
RUN mkdir /data
RUN chown 65210:65210 /data
EXPOSE 8080
USER 65210
CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]
