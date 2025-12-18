module "participant_password" {
  source           = "../random_string_generator"
  override_special = "!#$%&()-_=+[]{}<>?"
}

provider "postgresql" {
  host            = var.postgres_endpoint
  port            = var.postgres_port
  database        = "participants"
  username        = "dbadmin"
  password        = var.postgres_admin_password
  sslmode         = "require"
  connect_timeout = 15
  superuser       = false
}

resource "postgresql_role" "participant_user" {
  name     = var.participant
  login    = true
  password = module.participant_password.random_value
}

resource "postgresql_database" "participant_database" {
  name              = var.participant
  owner             = postgresql_role.participant_user.name
  lc_collate        = "en_US.UTF-8"
  lc_ctype          = "en_US.UTF-8"
  template          = "template0"
  allow_connections = true
}

resource "postgresql_grant" "participant_privs" {
  database    = postgresql_database.participant_database.name
  role        = postgresql_role.participant_user.name
  schema      = "public"
  object_type = "database"
  privileges  = ["ALL"]
}
