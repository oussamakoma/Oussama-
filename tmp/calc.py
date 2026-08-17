import csv

csv_data = """TYPE,TITLE_OR_NAME,CATEGORY_OR_DEBTTYPE,COST_OR_AMOUNT,SELLING_OR_PAID,DATE_TIMESTAMP,NOTES,DEVICE_MODEL,CUSTOMER_NAME,CREDIT_AMOUNT,CREDIT_PAID_DEPOSIT,WALLET,IS_DELIVERED
TRANSACTION,تغيير منفذ شحن ,PARTS,0.0,1200.0,1784332171242,,realme c21,براني في ليل,0.0,0.0,محفظة المحل,true
TRANSACTION,🌐 إنترنت,EXPENSE,520.0,0.0,1784309965861,انترنت,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,300.0,0.0,1784299224854,ماما رشيدة لاكريم,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,force reboot ,OTHER,0.0,400.0,1784238460621,,realme ,عبدقا جارنا,0.0,0.0,محفظة المحل,true
TRANSACTION,تغيير شاشة iphone 12 + flash,SCREEN,3800.0,6500.0,1784237434497,iPhone désactiver لقيته هاك او جبت افيشار او لقيت فيها خطوط,iphone 12,نوردين كنكاري,0.0,0.0,محفظة المحل,false
TRANSACTION,تغيير شاشة Samsung a13,SCREEN,2000.0,4500.0,1784237299404,افيشار 1800 او trap  مشكلة backlight صرات عندي غلطت ركبت sa04,Samsung a13,منير تاع زرق,0.0,0.0,محفظة المحل,false
TRANSACTION,تغيير شاشة oppo a94,SCREEN,8500.0,12000.0,1784237169809,,oppo a94,قاسم ميلفاي,0.0,0.0,محفظة المحل,false
TRANSACTION,activé batterie ,OTHER,0.0,1000.0,1784237079351,,Redmi  a2,محمد دوار قورين,0.0,0.0,محفظة المحل,true
TRANSACTION,work time import export contact,OTHER,0.0,200.0,1784235089623,,sam,بوعبدالله بلاتي,0.0,0.0,محفظة المحل,true
TRANSACTION,📦 أخرى ,EXPENSE,3700.0,0.0,1784198399114,flux 800 fil de  sode 2 1400 lita 183° 1500,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,450.0,0.0,1784198338031,نهار لي روحت مع سمير 4 بيتزا او سوفلي او ميرندا,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,config camera imou ,OTHER,0.0,500.0,1784146280673,,imou ,بن ذهيبة,0.0,0.0,محفظة المحل,true
TRANSACTION,دين شخصي: قادة راس,DEBT,1000.0,0.0,1784142145066,,,قادة راس,0.0,0.0,محفظة المحل,false
TRANSACTION,icloud بيلوما download app او نحيه,OTHER,0.0,500.0,1784067730540,,iphone ,ولد محمد منصور,0.0,0.0,محفظة المحل,true
TRANSACTION,hard rest ,OTHER,0.0,500.0,1784067677076,Contact afficher ,oppo,بريسلي محمد,0.0,0.0,محفظة المحل,true
TRANSACTION,nettoyage baf,OTHER,0.0,150.0,1784062767894,,oppo,مصطفى صغير,0.0,0.0,محفظة المحل,true
TRANSACTION,type c cable ,ACCESSORY,0.0,800.0,1784061853916,,,براني صغير,0.0,0.0,سلعة,true
TRANSACTION,🍔 غذاء,EXPENSE,30.0,0.0,1784061830917,ايسكمو,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,لي بقا من مصروف تاع عرس,OTHER,0.0,625.0,1783981050762,,,عبدنور,0.0,0.0,محفظة المحل,true
TRANSACTION,تغيير شاشة realme c55,SCREEN,2000.0,4500.0,1783979970965,,realme c55,جلول خو نواشة,0.0,0.0,محفظة المحل,true
TRANSACTION,📦 أخرى,EXPENSE,500.0,0.0,1783979883549,T7000 RELIFE,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,تغيير سارسو+ شاشة كاملة sam a51,SCREEN,2900.0,6500.0,1783979703300,,sam a51,ولد طلابي,2500.0,0.0,محفظة المحل,false
TRANSACTION,تغيير شاشاة انفينيكس+ باف + nap ⛔pixelated,SCREEN,2800.0,4500.0,1783978399857,كان طايح في ماء درتله nettoyage+ بدلته nap او باف لافيشار كي ركبتهاله فورسيت عليها شوية كاري تاع pixelated ,infinix smart 8,مونير تاع زرق,2500.0,0.0,محفظة المحل,true
TRANSACTION,🍔 غذاء,EXPENSE,50.0,0.0,1783968441800,حباش,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,160.0,0.0,1783968264755,ساندو او ساندو كوفريط او ايسكمو فلاش,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,30.0,0.0,1783960649302,قرعة ماء,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,📦 أخرى,EXPENSE,400.0,0.0,1783960571914,خسائر افيشار حباش صاحبي قوارة  p30 lite زوج افيشار 240 250 خلصت عليه 450,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🚌 مواصلات,EXPENSE,160.0,0.0,1783882472827,ركبا,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,con 9a/oppo شريتهم,INVENTORY,900.0,1225.0,1783882398714,,,ناصر,0.0,0.0,محفظة المحل,true
TRANSACTION,📦 أخرى,EXPENSE,650.0,0.0,1783882350085,COLLE T7000 110 ML,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,تغيير شاشة a13 + بطارية,SCREEN,3450.0,6800.0,1783881881529,تزيار لاتراب,sam a13,aziz amour,0.0,0.0,محفظة المحل,true
TRANSACTION,config new phone ,OTHER,0.0,400.0,1783880474261,,ace,نسيب مختار خالي,0.0,0.0,محفظة المحل,true
TRANSACTION,فورمات hard rest,OTHER,0.0,700.0,1783880441453,,,براني من قوارة,0.0,0.0,محفظة المحل,true
TRANSACTION,cable type c,ACCESSORY,0.0,700.0,1783880412237,,,براني,0.0,0.0,سلعة,true
TRANSACTION,cable types c,ACCESSORY,0.0,800.0,1783809514626,,,جلول فيلا ,0.0,0.0,سلعة,true
TRANSACTION,✂️ حلاقة,EXPENSE,500.0,0.0,1783632347852,hair cut ,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,✨ العناية الشخصية,EXPENSE,690.0,0.0,1783632293434,شامبو لام جيلات دوف,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🛒 تسوق,EXPENSE,4500.0,0.0,1783609661012,انسوبل,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,185.0,0.0,1783554122380,ساندو ساندو ساندو,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,كابلي لونا,ACCESSORY,0.0,200.0,1783554098403,,,ولد منداسي,0.0,0.0,سلعة,true
TRANSACTION,تغيير  منفذ شاحن ,PARTS,0.0,850.0,1783551178756,,9a,ولد منداسي,0.0,0.0,محفظة المحل,true
TRANSACTION,🤲 صدقة الأسرة,EXPENSE,120.0,0.0,1783546223601,ماما رشيده زوج لا كريم زوج دانون,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,حميشيش قتله من بعد نرجعلك او ديتهمله من دراهم كتمان 300,OTHER,0.0,200.0,1783542317275,,,,0.0,0.0,محفظة المحل,true
TRANSACTION,🍔 غذاء,EXPENSE,120.0,0.0,1783468511515,قابسة قاطو او كاندي,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,لاكريم,EXPENSE,250.0,0.0,1783465331836,انا او براهيم,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,عشا  برا انا او براهيم,EXPENSE,750.0,0.0,1783465207310,عطاني هو 250 هيا جات 100,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,كابل c,ACCESSORY,0.0,700.0,1783463439283,,,حمشيش,0.0,0.0,سلعة,true
TRANSACTION,🍔 غذاء,EXPENSE,180.0,0.0,1783463369035,روحنت ستيدية شرينا اتاي او كوكاو او شامية,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,frp,SERVICE,0.0,1600.0,1783440972115,,sam a03,خو عبد الله لي يبيع شراب ,0.0,0.0,محفظة المحل,true
TRANSACTION,تغيير شاشة ,SCREEN,2000.0,3500.0,1783440752114,,oppo a58,سمير خالي ,0.0,0.0,محفظة المحل,true
TRANSACTION,تغيير شاشة ,SCREEN,1850.0,3700.0,1783440712658,,sam j4 plus ,ولد مزاري,0.0,0.0,محفظة المحل,true
TRANSACTION,🍔 غذاء,EXPENSE,35.0,0.0,1783440382526,ايسكمو,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,270.0,0.0,1783379425092,2 ايسكمو ساندو او ساندو,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,تغيير شاشة +rest لحل تغير ic battery ,SCREEN,1900.0,5500.0,1783372501485,,redmi note 8pro,اب مرحوم تاع دوار لي عينه زرقين,500.0,0.0,محفظة المحل,true
TRANSACTION,cable iphone ,ACCESSORY,0.0,700.0,1783364327526,,,شباب تاع زرق,0.0,0.0,سلعة,true
TRANSACTION,تغيير شاشة,SCREEN,1800.0,4500.0,1783363495505,,sam m13,محمد زرق صاحب عمار ولد خالي,0.0,0.0,محفظة المحل,true
TRANSACTION,بيتزا,EXPENSE,270.0,0.0,1783350403726,بيتزا,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,80.0,0.0,1783339623595,وافرز,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,60.0,0.0,1783339597898,كورنيطو,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,crée complet icloud ,OTHER,0.0,250.0,1783290539664,,,ولد  طلابي,0.0,0.0,محفظة المحل,true
TRANSACTION,mise à jour tablette auto ,OTHER,0.0,700.0,1783284456236,,رونو,شريطي,200.0,200.0,محفظة المحل,true
TRANSACTION,شاحن لونا,ACCESSORY,0.0,1500.0,1783282226269,,,كحلالة,0.0,0.0,سلعة,true
TRANSACTION,تغيير منفذ شحن,PARTS,0.0,1200.0,1783282162594,,9a oronge,براني خو لعينيه خضرين لي لي خدمتله Redmi note,0.0,0.0,محفظة المحل,true
TRANSACTION,كيتمان bl,ACCESSORY,0.0,2000.0,1783273048448,,,حمشيش,600.0,600.0,سلعة,true
TRANSACTION,📦 أخرى,EXPENSE,1025.0,0.0,1783272909129,تودرت 🙅‍♂️⛔,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,cable luna ,ACCESSORY,0.0,600.0,1783211277887,بوه جا عندي قالي راني مرسول,,طاهر صاحبي,600.0,0.0,سلعة,true
TRANSACTION,🍔 غذاء,EXPENSE,50.0,0.0,1783211258917,كوكاو,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,تغيير منفذ type c,PARTS,0.0,700.0,1783211200158,,sam m13,عادل كاتبي,0.0,0.0,محفظة المحل,true
TRANSACTION,تغيير شاشة + on off power ,SCREEN,2000.0,4500.0,1783173060921,,sam m10,لي عنده طوبا يجي بطاطا,0.0,0.0,محفظة المحل,true
TRANSACTION,دين شخصي: مريم  ,DEBT,14900.0,0.0,1783172471909,قفطان 850 طالو 270 شناقل زوج 170 فليكسي 200,,مريم  ,0.0,0.0,محفظة المحل,false
TRANSACTION,🚌 مواصلات,EXPENSE,240.0,0.0,1783165053300,ركبا شيرات بريكو,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🚌 مواصلات,EXPENSE,120.0,0.0,1783163731927,ركبا طرام شيرات,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,130.0,0.0,1783125151846,اسكمو 2 او كوكاو,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🤲 صدقة ,EXPENSE,100.0,0.0,1783119199125,هذيك شيبانية تاع دوار,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,تنضيف او work time,OTHER,0.0,250.0,1783018842401,,,,0.0,0.0,محفظة المحل,true
TRANSACTION,☕ قهوة,EXPENSE,180.0,0.0,1783015548513,وافرز او ايزم,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,📦 أخرى,EXPENSE,400.0,0.0,1783005168993,ديليو,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,تغيير شاشة,SCREEN,2000.0,4700.0,1782935637669,,c21,حمد لي يدلقني نسيب طيطح,0.0,0.0,محفظة المحل,true
TRANSACTION,🍔 غذاء,EXPENSE,170.0,0.0,1782923949620,روش او ايزم,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,85.0,0.0,1782923755162,فلاش اسكمو  او بن ذهببة 4,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,تغيير الشاشه + بوشات,SCREEN,2000.0,4500.0,1782923564316,,9a,حكيم دنوني,500.0,0.0,محفظة المحل,true
TRANSACTION,🍔 غذاء,EXPENSE,120.0,0.0,1782895530383,2 ايسكمو 5 كوكاو,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,cable luna ,ACCESSORY,0.0,700.0,1782851573821,,,ولد قادة خو نوردين لافاش,0.0,0.0,سلعة,true
TRANSACTION,vérification internet ,OTHER,0.0,100.0,1782845727674,,,حبيب سكريبا,0.0,0.0,محفظة المحل,true
TRANSACTION,🍔 غذاء,EXPENSE,110.0,0.0,1782819852169,قرعة ماء,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,130.0,0.0,1782776495185,تانقو,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,force reboot exit recovery ,OTHER,0.0,200.0,1782766668165,,,عبدنور جارنا,0.0,0.0,محفظة المحل,true
TRANSACTION,بابا اعطيته 200;000 باه يخلص تصريح العرس رجع لي 60;000 30;000 عبد النور رجعتها من هذه تاع 60;000,OTHER,0.0,300.0,1782765360364,,,بابا,0.0,0.0,محفظة المحل,true
TRANSACTION,تغيير شاشة,SCREEN,2000.0,4700.0,1782759942977,,oppo a54,اصفر,0.0,0.0,محفظة المحل,true
TRANSACTION,🍔 غذاء,EXPENSE,120.0,0.0,1782723325295,بيمو,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,70.0,0.0,1782679015550,ساندو كوك,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,format,OTHER,0.0,200.0,1782677263133,,9a,خو خالد ستيلا,0.0,0.0,محفظة المحل,true
TRANSACTION,مضيعة وقت فيسبوك ,OTHER,0.0,130.0,1782671417918,,,توام,0.0,0.0,محفظة المحل,true
TRANSACTION,شحن فك و تركيب,OTHER,0.0,250.0,1782671382784,,9a,فرخ,0.0,0.0,محفظة المحل,true
TRANSACTION,frp,SERVICE,400.0,1500.0,1782667308540,,realme c63,بيلال كمارا,0.0,0.0,محفظة المحل,true
TRANSACTION,🍔 غذاء,EXPENSE,80.0,0.0,1782665524580,لاكريم,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,340.0,0.0,1782602816358,20 صدقة كوثر باقي انا,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,update all apps,OTHER,0.0,400.0,1782595532933,,,بن داحا,0.0,0.0,محفظة المحل,true
TRANSACTION,gmail ,OTHER,0.0,400.0,1782592822163,,,,0.0,0.0,محفظة المحل,true
TRANSACTION,☕ قهوة,EXPENSE,30.0,0.0,1782557640894,,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,390.0,0.0,1782557462606,وافرز كوك ياڨو شيكولا,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🛒 تسوق,EXPENSE,250.0,0.0,1782551358454,جافيل امير ,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,دين شخصي: بويا ,DEBT,2000.0,2000.0,1782524202966,,,بويا ,0.0,0.0,محفظة المحل,true
TRANSACTION,🍔 غذاء,EXPENSE,60.0,0.0,1782464995144,لاكريم,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,☕ قهوة,EXPENSE,90.0,0.0,1782455615776,1 بيض قهوة كاطو صغيرة,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,شاحن سيارة la tête,ACCESSORY,0.0,500.0,1782455560418,,,,0.0,0.0,سلعة,true
TRANSACTION,🍔 غذاء,EXPENSE,120.0,0.0,1782425943631,فلاش ساندو او ساندو كوفريط,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🚌 مواصلات,EXPENSE,500.0,0.0,1782425921028,رسلته يشريلي,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,تغيير شاشة+ سارسو60,SCREEN,2400.0,5500.0,1782425864759,,oppo a16,علولة حسين,0.0,0.0,محفظة المحل,true
TRANSACTION,تغير شاشة + frp,SCREEN,1800.0,5000.0,1782425801498,,oppo a17k,تاع زرق لي عندهم دوبلو,0.0,0.0,محفظة المحل,true
TRANSACTION,تنظيف لارام حاسوب انيتي,OTHER,0.0,700.0,1782387727125,,,طفل بريكو,0.0,0.0,محفظة المحل,true
TRANSACTION,force rest and clean tarp شاشة لمس تحبس,OTHER,0.0,200.0,1782385690526,بغا يخدم عندي افيشور صابني مبلع راه عند هواري,Sam a04,كرونطاي لي يخدم مع كمون,0.0,0.0,محفظة المحل,true
TRANSACTION,تغيير منفذ شحن,PARTS,0.0,600.0,1782385632764,,infinix تاع مرته,طاهر تاعنا,0.0,0.0,محفظة المحل,true
TRANSACTION,🍔 غذاء,EXPENSE,140.0,0.0,1782381997377,جوج بيض كرانتيكا فيتاجي,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,210.0,0.0,1782344550657,كوندي شوكو جوفريت 15,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,100.0,0.0,1782343207570,ساندو كوفريط او 5 كوكاو,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,اعادة  تثبيت منفذ شحن و اعادة تجديد قصدير ,OTHER,0.0,800.0,1782339626283,,sam,من كحلاليل عين زرقين,0.0,0.0,محفظة المحل,true
TRANSACTION,🍔 غذاء,EXPENSE,240.0,0.0,1782333332498,4000 كارانتيكا 2 كاشير ريكامار افروي صغيرة,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,50.0,0.0,1782256743649,كوكاو,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,دين شخصي: غشام,DEBT,100.0,0.0,1782256289317,,,غشام,0.0,0.0,محفظة المحل,false
TRANSACTION,cable luna,ACCESSORY,0.0,600.0,1782244920198,,,طاهر,0.0,0.0,محفظة المحل,true
TRANSACTION,تغيير منفذ الشحن oppo دروس,PARTS,0.0,1200.0,1782244865969,,,ولد صاحب عبدقا طالية,0.0,0.0,محفظة المحل,true
TRANSACTION,☕ قهوة,EXPENSE,170.0,0.0,1782186797585,قهوة و روش,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🌐 إنترنت,EXPENSE,2550.0,0.0,1782170222313,my ooredoo ,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,140.0,0.0,1782170158961,شيكولا صفرا او كوفريط تاع ساندو,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,50.0,0.0,1782166797554,كوكاو,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,دين شخصي: عبدنور,DEBT,4300.0,4300.0,1782145523062,خلصت livrere,,عبدنور,0.0,0.0,محفظة المحل,true
TRANSACTION,🍔 غذاء,EXPENSE,70.0,0.0,1782091569311,2 لاكريم,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,220.0,0.0,1782087136191,قرعه رامي زوج لاكريم واحده وافرس,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,110.0,0.0,1782087038333,قرعه ماء للناصر 3 قهوه عبد الكريم وانا  على لاكرام بنذهيبة,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,شراء مخزون,INVENTORY,2000.0,4500.0,1782074297819,,infinix Smart 9,تاع زرق لي عنده لاكونا,0.0,0.0,محفظة المحل,true
TRANSACTION,شاشة هاتف ,INVENTORY,1900.0,4500.0,1782074196313,,sam a03,عزيز ولد عمي,0.0,0.0,محفظة المحل,true
TRANSACTION,🚌 مواصلات,EXPENSE,450.0,0.0,1782074009519,عبدو ركبا راح شرالي,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,cable luna بوطونات,ACCESSORY,0.0,500.0,1782073705245,,,,0.0,0.0,سلعة,true
TRANSACTION,cable luna,ACCESSORY,0.0,600.0,1782072362053,,,,0.0,0.0,سلعة,true
TRANSACTION,دين شخصي: عبد النور,DEBT,1000.0,1000.0,1781985575125,قال لي تواتي القائد مات وجده 1000 دينار كان بيدي 50 مكنش صرف,,عبد النور,0.0,0.0,محفظة المحل,true
TRANSACTION,بيع هاتف استثمار: سامسونج بطونات,REFURB,1000.0,1500.0,1781983903428,بيع نقدي,سامسونج بطونات,اب امين بوغراسا قدور,0.0,0.0,الصندوق (Pocket),true
TRANSACTION,🍔 غذاء,EXPENSE,170.0,0.0,1781953245231,كاوكاو جنجلان قرعه ما كبيره جيريكا,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,230.0,0.0,1781912306165,دانون وافرز,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,🍔 غذاء,EXPENSE,120.0,0.0,1781909715417,حسين,,,0.0,0.0,مصروف شخصي,true
TRANSACTION,aux 2 كيتمان او كابل ايفون,ACCESSORY,0.0,1100.0,1781902584165,,,,0.0,0.0,سلعة,true
TRANSACTION,تنظيف منفذ شحن type c,OTHER,0.0,1200.0,1781902277533,حتى الغدوه باه خلصني خلصني,oppo a54, شارف لي عينيه خضرين تاع دوار,0.0,0.0,محفظة المحل,true
TRANSACTION,تغيير الشاشه,SCREEN,1800.0,4000.0,1781897728974,,9a,خو سليم تاع الدوار,0.0,0.0,محفظة المحل,true
TRANSACTION,دين شخصي: اسامه تاع الدوار,DEBT,500.0,500.0,1781897702053,في الليل جاء عندي وقال لي صرف لي 200 انا اعطيته 50,,اسامه تاع الدوار,0.0,0.0,محفظة المحل,true
TRANSACTION,شراء هاتف للاستثمار: سامسونج بطونات,REFURB,1000.0,0.0,1781897640000,شراء هاتف للاستثمار تدوير. رقم تسلسلي: غير مسجل,سامسونج بطونات,,0.0,0.0,الصندوق (Pocket),false
TRANSACTION,شاشه هاتف,SCREEN,2000.0,4500.0,1781896696337,,sam a03,دحيا,0.0,0.0,محفظة المحل,true
TRANSACTION,شاشه هاتف,INVENTORY,2000.0,2300.0,1781896651412,,smart 8,براهيم طالية,0.0,0.0,محفظة المحل,true"""

def get_profit(row):
    category = row['CATEGORY_OR_DEBTTYPE']
    is_delivered = row['IS_DELIVERED'] == 'true'
    cost_price = float(row['COST_OR_AMOUNT'] or 0.0)
    selling_price = float(row['SELLING_OR_PAID'] or 0.0)
    credit_amount = float(row['CREDIT_AMOUNT'] or 0.0)
    credit_paid = float(row['CREDIT_PAID_DEPOSIT'] or 0.0)
    is_prepaid = False  # By default in CSV we don't have isPrepaid or it's false
    
    if category == "EXPENSE":
        return -cost_price
    elif category == "DEBT":
        return selling_price - cost_price
    elif is_delivered or is_prepaid:
        if credit_amount > 0.0:
            total_paid = selling_price - credit_amount + credit_paid
            return total_paid - cost_price
        else:
            return selling_price - cost_price
    else:
        return -cost_price

pocket_transactions = []
personal_transactions = []
accessory_transactions = []
all_trans = []

reader = csv.DictReader(csv_data.splitlines())
for row in reader:
    row['profit'] = get_profit(row)
    all_trans.append(row)

# Calculate pocketChange
pocket_names = ["محفظة المحل", "مصروف الشهر", "الصندوق (Pocket)", ""]
personal_names = ["مصروف شخصي", "مصروفي شخصي", "مصروفي الشخصي"]

pocket_change = 0.0
personal_change = 0.0
accessory_change = 0.0

for t in all_trans:
    wallet = t['WALLET']
    cat = t['CATEGORY_OR_DEBTTYPE']
    title = t['TITLE_OR_NAME']
    selling_price = float(t['SELLING_OR_PAID'] or 0.0)
    profit = t['profit']
    
    is_accessory = (cat == "ACCESSORY")
    
    if is_accessory:
        accessory_change += profit
        continue
        
    if wallet in pocket_names or wallet == "الصندوق (Pocket)":
        if cat == "REFURB" and title.startswith("بيع"):
            val = selling_price
        else:
            val = profit
        pocket_change += val
        pocket_transactions.append((title, val))
        
    elif wallet in personal_names:
        if cat == "REFURB" and title.startswith("بيع"):
            val = selling_price
        else:
            val = profit
        personal_change += val
        personal_transactions.append((title, val))

print(f"Calculated Pocket Change: {pocket_change}")
print(f"Calculated Personal Change: {personal_change}")
print(f"Calculated Accessory (Goods) Change: {accessory_change}")
