import os
import re

def replace_in_file(path, replacements):
    if not os.path.exists(path): return
    with open(path, 'r') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(path, 'w') as f:
        f.write(content)

# MainActivity.kt
replace_in_file('app/src/main/java/com/example/MainActivity.kt', [
    ('if (it.category == "REFURB" && it.title.startsWith("بيع")) it.sellingPrice else it.profit', 'it.cashFlow'),
    ('.sumOf { it.profit }', '.sumOf { it.cashFlow }')
])

# SectionsScreen.kt - wallet balances use cashFlow
replace_in_file('app/src/main/java/com/example/ui/SectionsScreen.kt', [
    ('transactions.filter { it.category == "ACCESSORY" && it.affectBalance }.sumOf { it.profit }', 'transactions.filter { it.category == "ACCESSORY" && it.affectBalance }.sumOf { it.cashFlow }'),
    ('catTransactions.sumOf { it.profit }', 'catTransactions.sumOf { it.cashFlow }'),
    ('catTransactions.filter { isToday(it.date) }.sumOf { it.profit }', 'catTransactions.filter { isToday(it.date) }.sumOf { it.cashFlow }')
    # The rest are UI display of profit, keep as profit.
])

# IntroDashboardScreen.kt
replace_in_file('app/src/main/java/com/example/ui/IntroDashboardScreen.kt', [
    ('it.category == "DEBT" && it.profit > 0 }.sumOf { it.cashFlow }', 'it.category == "DEBT" && it.profit > 0 }.sumOf { it.profit }'),
    ('it.category == "DEBT" && it.profit < 0 }.sumOf { -it.cashFlow }', 'it.category == "DEBT" && it.profit < 0 }.sumOf { -it.profit }'),
    ('it.profit.coerceAtLeast(0.0)', 'it.cashFlow.coerceAtLeast(0.0)'), # Income from transaction -> cashFlow
    ('val p = if (it.category == "REFURB" && it.title.startsWith("بيع")) it.sellingPrice else it.profit', 'val p = it.cashFlow')
])
