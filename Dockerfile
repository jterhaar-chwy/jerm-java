FROM 278833423079.dkr.ecr.us-east-1.amazonaws.com/plat/java-baseimg:21-latest

# Install git and GitHub CLI (try different package managers)
USER root
RUN if command -v apk > /dev/null; then \
        # Alpine Linux
        apk add --no-cache git curl bash && \
        curl -fsSL https://github.com/cli/cli/releases/download/v2.40.1/gh_2.40.1_linux_amd64.tar.gz | tar -xz -C /tmp && \
        mv /tmp/gh_2.40.1_linux_amd64/bin/gh /usr/local/bin/; \
    elif command -v yum > /dev/null; then \
        # RHEL/CentOS
        yum install -y git curl && \
        curl -fsSL https://github.com/cli/cli/releases/download/v2.40.1/gh_2.40.1_linux_amd64.tar.gz | tar -xz -C /tmp && \
        mv /tmp/gh_2.40.1_linux_amd64/bin/gh /usr/local/bin/; \
    elif command -v microdnf > /dev/null; then \
        # Minimal RHEL/CentOS
        microdnf install -y git curl tar gzip && \
        curl -fsSL https://github.com/cli/cli/releases/download/v2.40.1/gh_2.40.1_linux_amd64.tar.gz | tar -xz -C /tmp && \
        mv /tmp/gh_2.40.1_linux_amd64/bin/gh /usr/local/bin/; \
    else \
        echo "No supported package manager found"; \
    fi

# Switch back to application user (if the base image has one)
USER chewyapp

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
