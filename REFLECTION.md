# Reflection

**Group members:**
Name(s) and student ID: Sandrine Pudukkattukaran Joy, 4743017

### 1. Enforceability of the One-Approval-Per-User Rule
Before authentication was introduced, user identity was completely client-asserted. The system relied on the `user_id` query parameter supplied in the HTTP request to identify the approver. Because there was no cryptographic signature or session verification, a client could spoof any user ID. Although a check prevented a user from approving their own review, this could be easily bypassed by submitting any other user's ID in the query parameter. Consequently, the database could not reliably enforce one-approval-per-user since the client could rotate through different user IDs arbitrarily to simulate approvals from multiple different users.

### 2. Remaining Security Weaknesses
Even with the new design, several security weaknesses persist:
1. **Lack of Token Revocation / Refresh Token Lifecycle**: The JWT tokens are stateless and expire after 15 minutes, but there is no mechanism to revoke a token once it has been issued. If a token is compromised, an attacker can use it freely until it expires. Additionally, the lack of refresh tokens forces users to re-submit their credentials frequently.
2. **Database Lookup overhead and Potential Side-Channel Attacks**: Resolving the bearer token requires loading the full user profile from the database to check permissions, which is vulnerable to timing side-channel attacks during password hashing comparison.
3. **No Role Hierarchy / Overly Permissive Admin Checks**: Since roles are mapped as a flat set without hierarchy, permissions are hardcoded checks like `hasAuthority('ROLE_MODERATOR')`. If an admin does not explicitly hold the moderator role, they cannot moderate reviews. This increases configuration complexity and makes privilege escalation easier if role tables are misconfigured.
