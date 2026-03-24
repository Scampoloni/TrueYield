export async function load({ locals }) {
    return {
        isAuthenticated: locals.isAuthenticated ?? false,
        user: locals.user ?? null
    };
}
