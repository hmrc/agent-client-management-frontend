# agent-client-management Frontend
aka "Manage who can deal with HMRC for you" or "Manage your tax agents" (MYTA)

[![Build Status](https://travis-ci.org/hmrc/agent-client-management-frontend.svg)](https://travis-ci.org/hmrc/agent-client-management-frontend) 


# What the service does
Provides a client side view of agent authorisations, accessed from a link in BTA or PTA.

They can:
- view their history of past relationships and any active ones
- remove an active authorisation for a specific agent and MTD service
- go to agent-invitations to accept/decline pending invitations

This allows the client to manage their agent(s) access to client data, and consent to act on their behalf with HMRC for a given MTD service.

Currently this service supports the following services:
- ITSA, including alternative ITSA (client does not need to be registered for ITSA)
- Personal Income Record
- MTD VAT
- Capital Gains Tax on UK property account
- Trusts or Estates (taxable and non-taxable)
- Plastic Packaging Tax

## Running the tests

    sbt test it/test

## Running the tests with coverage

    sbt clean coverageOn test it/test coverageReport

## Running the app locally

    sm2 -start AGENT_AUTHORISATION
    sm2 -stop AGENT_CLIENT_MANAGEMENT_FRONTEND
    sbt run

It should then be listening on port 9568

    browse http://localhost:9568/manage-your-tax-agents

### License


This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
