# Production Alpine Nginx Container for VibeCheck Web App
FROM nginx:1.25-alpine

# Security: Create non-root user and assign permissions
RUN touch /var/run/nginx.pid && \
    chown -R 1001:1001 /var/run/nginx.pid /var/cache/nginx /var/log/nginx

# Remove default nginx website
RUN rm -rf /usr/share/nginx/html/*

# Copy static assets
COPY web/ /usr/share/nginx/html/web/
COPY admin-portal/ /usr/share/nginx/html/admin-portal/
COPY partner-portal/ /usr/share/nginx/html/partner-portal/
COPY index.html /usr/share/nginx/html/index.html

# Copy custom nginx configuration
COPY web/nginx.conf /etc/nginx/conf.d/default.conf

# Security: Run as non-root user 1001
USER 1001

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
