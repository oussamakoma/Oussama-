#!/bin/bash
sed -i 's/it.isDelivered && it.category != "DEBT"/(it.isDelivered || it.isPrepaid) \&\& it.category != "DEBT"/' app/src/main/java/com/example/ui/viewmodel/WorkshopViewModel.kt
sed -i 's/it.category != "DEBT" &&/it.category != "DEBT" \&\&\n            (it.isDelivered || it.isPrepaid) \&\&/' app/src/main/java/com/example/ui/viewmodel/WorkshopViewModel.kt
sed -i 's/if (it.category == "REFURB" && it.title.startsWith("بيع")) it.sellingPrice else it.profit/if (it.category == "REFURB" \&\& it.title.startsWith("بيع")) it.sellingPrice else it.accountingProfit/g' app/src/main/java/com/example/ui/viewmodel/WorkshopViewModel.kt
