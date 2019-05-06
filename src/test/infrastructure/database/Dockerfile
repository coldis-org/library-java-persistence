# From Postgres.
FROM coldis/transactional-repository

# Copies the server configuration.
COPY conf/*.conf /tmp/
RUN chown -R postgres:postgres /tmp/
