import re

with open('app/src/main/java/com/example/ui/IntroDashboardScreen.kt', 'r') as f:
    lines = f.readlines()

for i in range(len(lines)):
    line = lines[i]
    if 'todayMaintenanceProfit' in line or 'monthMaintenanceProfit' in line:
        lines[i] = line.replace('.sumOf { it.cashFlow }', '.sumOf { it.profit }')
    elif 'debtRevenue' in line or 'negativeDebt' in line or 'it.category == "DEBT"' in line:
        lines[i] = line.replace('.sumOf { it.cashFlow }', '.sumOf { it.profit }')
        lines[i] = line.replace('.sumOf { -it.cashFlow }', '.sumOf { -it.profit }')

with open('app/src/main/java/com/example/ui/IntroDashboardScreen.kt', 'w') as f:
    f.writelines(lines)
