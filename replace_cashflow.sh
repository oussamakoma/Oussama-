#!/bin/bash

# Update WorkshopTransaction.kt
sed -i 's/val accountingProfit: Double/val cashFlow: Double\n        get() {\n            if (category == "EXPENSE") return -costPrice\n            if (category == "DEBT") return (sellingPrice - costPrice)\n            if (category == "REFURB") {\n                if (title.startsWith("بيع")) {\n                    if (creditAmount > 0.0) {\n                        return sellingPrice - creditAmount + creditPaid\n                    }\n                    return sellingPrice\n                } else {\n                    return -costPrice\n                }\n            }\n            if (isDelivered || isPrepaid) {\n                if (creditAmount > 0.0) {\n                    val totalPaid = sellingPrice - creditAmount + creditPaid\n                    return totalPaid - costPrice\n                } else {\n                    return sellingPrice - costPrice\n                }\n            }\n            return -costPrice\n        }\n\n    val accountingProfit: Double/g' app/src/main/java/com/example/data/model/WorkshopTransaction.kt

# Let's also fix the profit property directly
cat << 'INNER_EOF' > fix_profit.py
import re

with open('app/src/main/java/com/example/data/model/WorkshopTransaction.kt', 'r') as f:
    content = f.read()

new_profit_block = """    val profit: Double
        get() {
            if (category == "EXPENSE") return -costPrice
            if (category == "DEBT") return (sellingPrice - costPrice)
            if (category == "REFURB") {
                if (title.startsWith("بيع")) {
                    if (creditAmount > 0.0) {
                        val totalPaid = sellingPrice - creditAmount + creditPaid
                        return totalPaid - costPrice
                    }
                    return sellingPrice - costPrice
                } else {
                    return 0.0
                }
            }
            if (isDelivered || isPrepaid) {
                if (creditAmount > 0.0) {
                    val totalPaid = sellingPrice - creditAmount + creditPaid
                    return totalPaid - costPrice
                } else {
                    return sellingPrice - costPrice
                }
            }
            return 0.0
        }"""

content = re.sub(r'    val profit: Double.*?else -costPrice', new_profit_block, content, flags=re.DOTALL)
content = re.sub(r'    val accountingProfit: Double.*?else 0\.0', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/model/WorkshopTransaction.kt', 'w') as f:
    f.write(content)
INNER_EOF
python3 fix_profit.py

# Replace in IntroDashboardScreen.kt
sed -i 's/if (it.category == "REFURB" && it.title.startsWith("بيع")) it.sellingPrice else it.profit/it.cashFlow/g' app/src/main/java/com/example/ui/IntroDashboardScreen.kt
sed -i 's/\.sumOf { it.profit }/.sumOf { it.cashFlow }/g' app/src/main/java/com/example/ui/IntroDashboardScreen.kt

# Except for these lines in IntroDashboardScreen where it should be profit:
# 167: todayMaintenanceProfit -> sumOf { it.profit }
# 178: monthMaintenanceProfit -> sumOf { it.profit }
# 272, 293, 497, 522 -> DEBT profit -> sumOf { it.profit } / {-it.profit}
