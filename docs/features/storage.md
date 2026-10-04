# Storage and synchronization

Use local storage plus a cloud database. The approved scaffold stack is Room and Firebase; Firestore is the intended database. Preserve available phone measurements offline without waiting for the backend.

Store source-tagged raw readings separately from calculated results and reversible user edits. Keep crop/video evidence where policy allows; decide file retention independently from structured samples. Source restrictions still apply to local copies and cloud copies.

Navigation-derived retention must follow [navigation constraints](../integrations/navigation.md). Independent phone observations are a separate source, not a way to relabel API data.

Open: authentication, sync triggers/batching, retries, offline reconciliation, data schema evolution, video location, per-source retention and reprocessing. Firebase Cloud Functions, Vercel Functions and Pub/Sub are implementation options only if required; approving the scaffold does not provision or deploy services.
