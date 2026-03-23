export async function handle({ event, resolve }) {
    const jwt_token = event.cookies.get('jwt_token');
    const user_info_raw = event.cookies.get('user_info');

    if (jwt_token && user_info_raw) {
        try {
            event.locals.jwt_token = jwt_token;
            event.locals.user = JSON.parse(user_info_raw);
            event.locals.isAuthenticated = true;
        } catch {
            event.locals.isAuthenticated = false;
            event.locals.user = null;
            event.locals.jwt_token = null;
        }
    } else {
        event.locals.isAuthenticated = false;
        event.locals.user = null;
        event.locals.jwt_token = null;
    }

    // Protect main app routes — redirect to /login if not authenticated
    const protectedPaths = ['/', '/portfolios', '/holdings', '/audit', '/account'];
    const isProtected = protectedPaths.some(
        (p) => event.url.pathname === p || event.url.pathname.startsWith(p + '/')
    );

    if (isProtected && !event.locals.isAuthenticated) {
        return new Response(null, {
            status: 302,
            headers: { Location: '/login' }
        });
    }

    return resolve(event);
}
