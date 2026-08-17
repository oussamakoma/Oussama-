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
