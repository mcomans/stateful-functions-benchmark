createdb --host postgresql --user postgres benchmark-orleans
psql --host postgresql --user postgres benchmark-orleans < PostgreSQL-Main.sql
psql --host postgresql --user postgres benchmark-orleans < PostgreSQL-Persistence.sql
psql --host postgresql --user postgres benchmark-orleans < PostgreSQL-Clustering.sql
