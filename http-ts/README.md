# TypeDB HTTP Typescript Driver

## Driver Architecture

To learn about how the TypeDB HTTP driver communicates with the TypeDB Server,
refer to the [HTTP API Reference](https://typedb.com/docs/reference/http-api).

## API Reference

To learn about the methods available for executing queries and retrieving their answers using Typescript, refer to
the [API Reference](https://typedb.com/docs/reference/http-drivers/typescript).

## Install TypeDB HTTP Typescript Driver through NPM

1. Install `@typedb/driver-http` through npm:

```bash
npm install @typedb/driver-http
```

2. Make sure a [TypeDB Server](https://typedb.com/docs/home/install/) is
   running.
3. Use TypeDB Driver in your program:

```ts
import { TypeDBHttpDriver, isApiErrorResponse } from "@typedb/driver-http";

const driver = new TypeDBHttpDriver({
    username: "admin",
    password: "password",
    addresses: [ "localhost:1729" ],
});

const transactionResponse = await driver.openTransaction("database-name", "read");
if (isApiErrorResponse(transactionResponse)) throw transactionResponse.err;
const transactionId = transactionResponse.ok.transactionId;

const answerResponse = await driver.query(transactionId, "match entity $x;");
if (isApiErrorResponse(answerResponse)) throw answerResponse.err;
const answer = answerResponse.ok;

if (answer.answerType === "conceptRows") {
   answer.answers.forEach((row) => {
        console.log(row.data)
    })
}
```

To connect over TLS to a server whose certificate is signed by a private CA, pass the CA certificate file in Node.js:

```ts
const driver = new TypeDBHttpDriver({
    username: "admin",
    password: "password",
    addresses: [ "https://typedb.example.com:8000" ],
    tls: { rootCAPath: "ca.pem" },
});
```

In browsers, add the CA to the operating system's or the browser's trust store instead.
