# agent-client-management Frontend

[ ![Download](https://api.bintray.com/packages/hmrc/releases/agent-client-management-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/agent-client-management-frontend/_latestVersion)

## Running the tests

    sbt test it:test

## Running the tests with coverage

    sbt clean coverageOn test it:test coverageReport

## Running the app locally

    sm --start AGENT_MTD -f
    sm --stop AGENT_CLIENT_MANAGEMENT_FRONTEND
    sbt run

It should then be listening on port 9568

    browse http://localhost:9568/agent-client-management

### License


This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
