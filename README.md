SpEL Injection Demonstration
===

The following project is an intentionally-vulnerable application designed to demonstrate
the hazards of composing SpEL expressions from untrusted data, specifically user input.

There are four endpoints that can be hit:

*/widget/search* - This endpoint does not exercise SpEL and is thus impermeable

*/widget/unsafe-search* - This endpoint uses `StandardEvaluationContext`, which grants access
to `Runtime`. The endpoint doesn't verify user input, taking that user input and composing it
into a SpEL expression.

*/widget/safer-search* - This endpoint uses `SimpleEvaluationContext`, which guards access to
`Runtime` by default. The endpoint still doesn't verify user input, though, meaning that several
attacks are still possible.

*/widget/safest-search* - This endpoint uses `SimpleEvaluationContext` as well as introduces a
whitelist guard against malicious user input.

Testing
----

There are unit tests that demonstrate exploits against these endpoints.

Also, the application can be hit directly:

```
curl http://localhost:8080/widget/unsafe-search?term=Widget\&filter=price%20lt%2020`
```
