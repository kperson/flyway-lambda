# Flyway Lambda Migrations

## MySQL Example using RDS Data API Config

```hcl
module "my_db_schema" {
  source              = "github.com/kperson/flyway-lambda//terraform"
  function_name       = "<my_migration_function>"
  db                  = "<my_database_name>"
  secrets_manager_arn = "<my_rds_data_app_formatted_secrets_arn>"
  kms_key_arn         = "<kms_key_use_for_secrets_arn">
  migration_location  = "<location_of_flyway_migration_files>"
  subnet_ids          = ["<subnet_id_for_lambda">"]
  security_group_ids  = ["<security_group_id_for_lambda>"]
}
```