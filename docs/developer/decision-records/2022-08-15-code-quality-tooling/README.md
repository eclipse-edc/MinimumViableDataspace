# Code Quality

## Decision

Use [Checkstyle](https://checkstyle.sourceforge.io/) and [CodeQL](https://codeql.github.com/) static code analysis tools to guarantee adherence to coding standards and discover potential vulnerabilities.

## Rationale

Both Checkstyle and CodeQL are already in use in the EDC framework, so these tools are the natural choice in related projects like the MVD as well.

## Approach

Checkstyle checks and CodeQL analysis need to run on Pull Requests in both upstream and forked repositories.
