variable "migration_location" {
  type = string
}

variable "kms_key_arn" {
  type = string
}


variable "function_name" {
  type = string
}

variable "subnet_ids" {
  type = list(string)
}

variable "security_group_ids" {
  type = list(string)
}

variable "secrets_manager_arn" {
  type = string
}

variable "db" {
  type = string
}

resource "random_string" "working_dir" {
  length  = 10
  upper   = false
  number  = false
  special = false
  keepers = {
    time = timestamp()
  }
}

# Task Role
data "aws_iam_policy_document" "assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "lambda" {

  statement {
    actions = [
      "logs:*"
    ]

    resources = [
      "*"
    ]
  }

  statement {
    actions = [
      "secretsmanager:GetSecretValue",
    ]

    resources = [
      var.secrets_manager_arn
    ]
  }

  statement {
    actions = [
      "ec2:DescribeInstances",
      "ec2:CreateNetworkInterface",
      "ec2:AttachNetworkInterface",
      "ec2:DescribeNetworkInterfaces",
      "ec2:DeleteNetworkInterface"
    ]

    resources = [
      "*"
    ]
  }

  statement {
    actions = [
      "kms:Decrypt"
    ]

    resources = [
      var.kms_key_arn
    ]
  }

  

}

resource "aws_iam_role" "lambda" {
  assume_role_policy = data.aws_iam_policy_document.assume_role.json
}


resource "aws_iam_policy" "lambda" {
  policy = data.aws_iam_policy_document.lambda.json
}

resource "aws_iam_role_policy_attachment" "lambda" {
  role       = aws_iam_role.lambda.name
  policy_arn = aws_iam_policy.lambda.arn
}


#hack, we need to wait until the attachements are complete
data "template_file" "role_arn" {
  depends_on = [aws_iam_role_policy_attachment.lambda]
  template   = aws_iam_role.lambda.arn
  vars = {
  }
}

resource "null_resource" "create_working_dir" {
  triggers = {
    timestamp = timestamp()
  }
  provisioner "local-exec" {
    command = format("mkdir -p %s/sql && mkdir -p %s/lib", random_string.working_dir.result, random_string.working_dir.result)
    environment = {
    }
  }
}

resource "null_resource" "add_sql" {
  triggers = {
    timestamp = timestamp()
  }
  depends_on = [null_resource.create_working_dir]
  provisioner "local-exec" {
    command = format("cp -R %s/* %s/sql", var.migration_location, random_string.working_dir.result)
    environment = {
    }
  }
}

resource "null_resource" "add_migration_script" {
  triggers = {
    timestamp = timestamp()
  }
  depends_on = [null_resource.create_working_dir]
  provisioner "local-exec" {
    command = format("cp %s/flyway-migrate.jar %s/lib/flyway-migrate.jar", path.module, random_string.working_dir.result)
    environment = {
    }
  }
}

data "archive_file" "zip" {
  depends_on  = [null_resource.add_migration_script, null_resource.add_sql]
  type        = "zip"
  source_dir  = random_string.working_dir.result
  output_path = format("%s.zip", random_string.working_dir.result)
}

resource "aws_lambda_function" "lambda" {
  filename         = format("%s.zip", random_string.working_dir.result)
  function_name    = var.function_name
  role             = data.template_file.role_arn.rendered
  handler          = "com.github.kperson.flyway.Lambda"
  runtime          = "java11"
  memory_size      = 2048
  timeout          = 900
  publish          = true
  source_code_hash = data.archive_file.zip.output_base64sha256

  vpc_config {
    subnet_ids         = var.subnet_ids
    security_group_ids = var.security_group_ids
  }

  environment {
    variables = {
      SECRETS_MANAGER_ARN = var.secrets_manager_arn
      DB                  = var.db
    }
  }
}

output "function_arn" {
  value = aws_lambda_function.lambda.arn
}

output "function_name" {
  value = aws_lambda_function.lambda.function_name
}

resource "null_resource" "cleanup" {
  triggers = {
    source_code_hash = data.archive_file.zip.output_base64sha256
  }
  depends_on = [aws_lambda_function.lambda]
  provisioner "local-exec" {
    command = format("rm -rf %s && rm %s.zip", random_string.working_dir.result, random_string.working_dir.result)
    environment = {
    }
  }
}