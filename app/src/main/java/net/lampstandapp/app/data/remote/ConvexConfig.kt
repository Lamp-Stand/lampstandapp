package net.lampstandapp.app.data.remote

/**
 * Direct Convex client API (no custom bridge needed).
 * - Client API base: https://<deploy>.convex.cloud  (query/mutation/action)
 * - Site URL (httpActions, not used in Phase 1): https://<deploy>.convex.site
 *
 * exciting-dodo-656 is the shared CLOUD backend: the web app
 * (projectLampStand/.env.local) points at the same deployment, so web + mobile
 * share one database and one set of users. Point both here unless you are
 * deliberately testing against a local `npx convex dev` backend.
 */
object ConvexConfig {
    var cloudUrl: String = "https://exciting-dodo-656.convex.cloud/"
    var siteUrl: String = "https://exciting-dodo-656.convex.site/"
}
