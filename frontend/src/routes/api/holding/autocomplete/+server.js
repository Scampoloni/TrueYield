import { json } from '@sveltejs/kit';

export async function GET({ url, locals }) {
    if (!locals.jwt_token) return json([], { status: 401 });

    const q = url.searchParams.get('q')?.trim() ?? '';
    if (q.length < 1) return json([]);

    try {
        const res = await fetch(
            `https://query1.finance.yahoo.com/v1/finance/search?q=${encodeURIComponent(q)}&quotesCount=7&newsCount=0&enableFuzzyQuery=false`,
            { headers: { 'User-Agent': 'Mozilla/5.0' } }
        );
        if (!res.ok) return json([]);

        const data = await res.json();
        const quotes = /** @type {any[]} */ (data.quotes ?? [])
            .filter((r) => r.quoteType === 'EQUITY' || r.quoteType === 'ETF')
            .map((r) => ({
                symbol: r.symbol ?? '',
                name: r.longname || r.shortname || r.symbol || '',
                exchange: r.exchange ?? '',
                type: r.quoteType ?? 'EQUITY'
            }));
        return json(quotes);
    } catch {
        return json([]);
    }
}
