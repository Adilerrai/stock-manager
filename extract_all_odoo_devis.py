import json
import urllib.request
import ssl

BASE_URL = 'https://sit-transmission1.odoo.com'
COOKIE = 'frontend_lang=fr_FR; tz=Africa/Casablanca; cids=1; color_scheme=dark; session_id=nuqD8NS9iu8FFLbsthUzdVKOmdzs-UvDkW53NORh7alqCaUuPpRN5Q6wAo8bJ1SbDcwl7PvTdczFhHensqXhhQ'
HEADERS = {
    'Accept': 'application/json',
    'Content-Type': 'application/json',
    'Cookie': COOKIE,
    'Origin': BASE_URL,
    'Referer': f'{BASE_URL}/odoo',
}
CTX = ssl.create_default_context()

def odoo_call(model, method, args, kwargs):
    url = f"{BASE_URL}/web/dataset/call_kw/{model}/{method}"
    payload = {'jsonrpc': '2.0', 'method': 'call', 'params': {'model': model, 'method': method, 'args': args, 'kwargs': kwargs}}
    req = urllib.request.Request(url, data=json.dumps(payload).encode('utf-8'), headers=HEADERS)
    with urllib.request.urlopen(req, context=CTX) as resp:
        res = json.loads(resp.read().decode('utf-8'))
        if 'error' in res:
            raise RuntimeError(f"Odoo Error {model}.{method}: {res['error']}")
        return res.get('result', {})

def main():
    print("1. Extraction de tous les Devis / Commandes (sale.order) depuis Odoo...")
    orders = odoo_call('sale.order', 'search_read', [[]], {
        'fields': [
            'id', 'name', 'date_order', 'partner_id', 'state',
            'amount_untaxed', 'amount_tax', 'amount_total', 'client_order_ref', 'order_line'
        ],
        'order': 'name asc',
        'limit': 5000
    })
    print(f"   -> {len(orders)} sale.order trouvés dans Odoo.")

    all_line_ids = []
    for o in orders:
        all_line_ids.extend(o.get('order_line', []))

    print(f"2. Extraction des lignes ({len(all_line_ids)} sale.order.line)...")
    lines = []
    batch_size = 800
    for i in range(0, len(all_line_ids), batch_size):
        chunk = all_line_ids[i:i+batch_size]
        res = odoo_call('sale.order.line', 'search_read', [[['id', 'in', chunk]]], {
            'fields': [
                'id', 'order_id', 'product_id', 'name', 'product_uom_qty',
                'price_unit', 'discount', 'price_subtotal', 'price_total', 'display_type'
            ]
        })
        lines.extend(res)

    prod_ids = list(set(l['product_id'][0] for l in lines if l.get('product_id')))
    print(f"3. Extraction des produits ({len(prod_ids)} produits uniques)...")
    prods = {}
    for i in range(0, len(prod_ids), batch_size):
        chunk = prod_ids[i:i+batch_size]
        res = odoo_call('product.product', 'search_read', [[['id', 'in', chunk]]], {
            'fields': ['id', 'default_code', 'name']
        })
        for pr in res:
            prods[pr['id']] = {
                'code': (pr.get('default_code') or '').strip(),
                'name': (pr.get('name') or '').strip()
            }

    lines_by_order = {}
    for l in lines:
        if l.get('display_type'):
            continue
        if not l.get('product_id'):
            continue
        oid = l['order_id'][0]
        pid = l['product_id'][0]
        pinfo = prods.get(pid, {'code': '', 'name': l['product_id'][1]})
        lines_by_order.setdefault(oid, []).append({
            'line_id': l['id'],
            'product_id': pid,
            'product_code': pinfo['code'],
            'product_name': pinfo['name'],
            'description': l.get('name') or '',
            'qty': float(l.get('product_uom_qty') or 0.0),
            'price_unit': float(l.get('price_unit') or 0.0),
            'discount': float(l.get('discount') or 0.0),
            'subtotal': float(l.get('price_subtotal') or 0.0),
            'total': float(l.get('price_total') or 0.0),
        })

    result = []
    for o in orders:
        oid = o['id']
        result.append({
            'id': oid,
            'name': o['name'],
            'state': o['state'],
            'date_order': o.get('date_order') or '',
            'client': o['partner_id'][1] if o.get('partner_id') else '',
            'client_order_ref': o.get('client_order_ref') or '',
            'amount_untaxed': float(o.get('amount_untaxed') or 0.0),
            'amount_tax': float(o.get('amount_tax') or 0.0),
            'amount_total': float(o.get('amount_total') or 0.0),
            'lignes': lines_by_order.get(oid, [])
        })

    with open('odoo_all_sale_orders.json', 'w', encoding='utf-8') as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    print(f"[OK] Sauvegardé {len(result)} commandes/devis dans odoo_all_sale_orders.json")

if __name__ == '__main__':
    main()
