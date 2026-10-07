const menuNames = {
  "王老吉": ["Wanglaoji herbal tea", "Thé aux plantes Wanglaoji"], "北冰洋": ["Beibingyang soft drink", "Soda Beibingyang"], "雪花啤酒": ["Snow beer", "Bière Snow"],
  "米饭": ["Steamed rice", "Riz vapeur"], "馒头": ["Steamed wheat bun", "Pain de blé à la vapeur"], "老坛酸菜鱼": ["Fish with fermented mustard greens", "Poisson aux feuilles de moutarde fermentées"],
  "经典酸菜鮰鱼": ["Catfish with fermented mustard greens", "Poisson-chat aux feuilles de moutarde fermentées"], "蜀味水煮草鱼": ["Sichuan-style poached grass carp", "Carpe herbivore pochée à la sichuanaise"],
  "清炒小油菜": ["Stir-fried bok choy", "Pak-choï sauté"], "蒜蓉娃娃菜": ["Baby Chinese cabbage with garlic", "Chou chinois à l’ail"], "清炒西兰花": ["Stir-fried broccoli", "Brocoli sauté"],
  "炝炒圆白菜": ["Stir-fried cabbage", "Chou sauté"], "清蒸鲈鱼": ["Steamed bass", "Bar à la vapeur"], "东坡肘子": ["Dongpo braised pork knuckle", "Jarret de porc braisé Dongpo"],
  "梅菜扣肉": ["Pork belly with preserved mustard greens", "Poitrine de porc aux feuilles de moutarde conservées"], "剁椒鱼头": ["Fish head with chopped chili", "Tête de poisson au piment haché"],
  "金汤酸菜牛蛙": ["Frog with mustard greens in golden broth", "Grenouille aux feuilles de moutarde dans un bouillon doré"], "香锅牛蛙": ["Aromatic dry-pot frog", "Grenouille en cocotte aux aromates"], "馋嘴牛蛙": ["House-special frog", "Grenouille façon maison"],
  "草鱼2斤": ["Grass carp (1 kg)", "Carpe herbivore (1 kg)"], "江团鱼2斤": ["Jiangtuan catfish (1 kg)", "Poisson-chat Jiangtuan (1 kg)"], "鮰鱼2斤": ["Catfish (1 kg)", "Poisson-chat (1 kg)"],
  "鸡蛋汤": ["Egg soup", "Soupe aux œufs"], "平菇豆腐汤": ["Oyster mushroom and tofu soup", "Soupe aux pleurotes et au tofu"], "套餐A": ["Meal combo A", "Menu A"], "人气套餐A": ["Popular meal combo A", "Menu populaire A"], "商务套餐A": ["Business meal combo A", "Menu d’affaires A"]
};

const categories = {
  "蜀味牛蛙": ["Sichuan-style frog", "Grenouille à la sichuanaise"], "蜀味烤鱼": ["Sichuan-style grilled fish", "Poisson grillé à la sichuanaise"], "特色蒸菜": ["Steamed specialties", "Spécialités à la vapeur"],
  "新鲜时蔬": ["Seasonal vegetables", "Légumes de saison"], "水煮鱼": ["Sichuan poached fish", "Poisson poché à la sichuanaise"], "传统主食": ["Traditional staples", "Féculents traditionnels"],
  "酒水饮料": ["Drinks", "Boissons"], "汤类": ["Soups", "Soupes"], "人气套餐": ["Popular combos", "Menus populaires"], "商务套餐": ["Business combos", "Menus affaires"]
};

const descriptions = {
  "还是小时候的味道": ["The familiar taste of childhood", "La saveur de votre enfance"], "精选五常大米": ["Selected Wuchang rice", "Riz sélectionné de Wuchang"], "优质面粉": ["Made with quality flour", "Préparé avec une farine de qualité"],
  "原料：汤，草鱼，酸菜": ["Ingredients: broth, grass carp, fermented mustard greens", "Ingrédients : bouillon, carpe herbivore, feuilles de moutarde fermentées"],
  "原料：酸菜，江团，鮰鱼": ["Ingredients: fermented mustard greens, Jiangtuan catfish, catfish", "Ingrédients : feuilles de moutarde fermentées, poisson-chat Jiangtuan, poisson-chat"],
  "原料：草鱼，汤": ["Ingredients: grass carp, broth", "Ingrédients : carpe herbivore, bouillon"], "原料：小油菜": ["Ingredients: bok choy", "Ingrédients : pak-choï"],
  "原料：蒜，娃娃菜": ["Ingredients: garlic, baby Chinese cabbage", "Ingrédients : ail, chou chinois"], "原料：西兰花": ["Ingredients: broccoli", "Ingrédients : brocoli"], "原料：圆白菜": ["Ingredients: cabbage", "Ingrédients : chou"],
  "原料：鲈鱼": ["Ingredients: bass", "Ingrédients : bar"], "原料：猪肘棒": ["Ingredients: pork knuckle", "Ingrédients : jarret de porc"], "原料：猪肉，梅菜": ["Ingredients: pork, preserved mustard greens", "Ingrédients : porc, feuilles de moutarde conservées"],
  "原料：鲢鱼，剁椒": ["Ingredients: silver carp, chopped chili", "Ingrédients : carpe argentée, piment haché"], "原料：鲜活牛蛙，酸菜": ["Ingredients: fresh frog, fermented mustard greens", "Ingrédients : grenouille fraîche, feuilles de moutarde fermentées"],
  "配料：鲜活牛蛙，莲藕，青笋": ["Ingredients: fresh frog, lotus root, stem lettuce", "Ingrédients : grenouille fraîche, racine de lotus, laitue-tige"],
  "配料：鲜活牛蛙，丝瓜，黄豆芽": ["Ingredients: fresh frog, sponge gourd, soybean sprouts", "Ingrédients : grenouille fraîche, courge luffa, pousses de soja"],
  "原料：草鱼，黄豆芽，莲藕": ["Ingredients: grass carp, soybean sprouts, lotus root", "Ingrédients : carpe herbivore, pousses de soja, racine de lotus"],
  "配料：江团鱼，黄豆芽，莲藕": ["Ingredients: Jiangtuan catfish, soybean sprouts, lotus root", "Ingrédients : poisson-chat Jiangtuan, pousses de soja, racine de lotus"],
  "原料：鮰鱼，黄豆芽，莲藕": ["Ingredients: catfish, soybean sprouts, lotus root", "Ingrédients : poisson-chat, pousses de soja, racine de lotus"],
  "配料：鸡蛋，紫菜": ["Ingredients: egg, nori seaweed", "Ingrédients : œuf, algue nori"], "配料：豆腐，平菇": ["Ingredients: tofu, oyster mushrooms", "Ingrédients : tofu, pleurotes"]
};

const shopAddresses = {
  "北京市朝阳区新街大道1号": ["No. 1 Xinjie Avenue, Chaoyang District, Beijing", "N° 1, avenue Xinjie, district de Chaoyang, Pékin"],
  "北京市朝阳区新街大道一号楼8层": ["8th floor, Building 1, Xinjie Avenue, Chaoyang District, Beijing", "8e étage, bâtiment 1, avenue Xinjie, district de Chaoyang, Pékin"]
};

const flavorNames = {
  "不要葱": ["No scallions", "Sans ciboule"], "不要香菜": ["No cilantro", "Sans coriandre"], "不辣": ["Not spicy", "Non épicé"],
  "微辣": ["Mild", "Légèrement épicé"], "中辣": ["Medium spicy", "Moyennement épicé"], "特辣": ["Very spicy", "Très épicé"],
  "重辣": ["Extra spicy", "Très épicé"], "不要蒜": ["No garlic", "Sans ail"], "不要辣": ["No chili", "Sans piment"],
  "忌口": ["Dietary preferences", "Préférences alimentaires"], "辣度": ["Spice level", "Niveau de piment"], "口味": ["Flavor", "Saveur"]
};

const regions = {
  "北京市": ["Beijing", "Pékin"], "天津市": ["Tianjin", "Tianjin"], "上海市": ["Shanghai", "Shanghai"], "重庆市": ["Chongqing", "Chongqing"],
  "市辖区": ["Urban district", "District urbain"], "朝阳区": ["Chaoyang District", "district de Chaoyang"], "丰台区": ["Fengtai District", "district de Fengtai"],
  "海淀区": ["Haidian District", "district de Haidian"], "东城区": ["Dongcheng District", "district de Dongcheng"], "西城区": ["Xicheng District", "district de Xicheng"],
  "石景山区": ["Shijingshan District", "district de Shijingshan"], "通州区": ["Tongzhou District", "district de Tongzhou"], "昌平区": ["Changping District", "district de Changping"],
  "河北省": ["Hebei", "Hebei"], "山西省": ["Shanxi", "Shanxi"], "内蒙古自治区": ["Inner Mongolia", "Mongolie-Intérieure"], "辽宁省": ["Liaoning", "Liaoning"],
  "吉林省": ["Jilin", "Jilin"], "黑龙江省": ["Heilongjiang", "Heilongjiang"], "江苏省": ["Jiangsu", "Jiangsu"], "浙江省": ["Zhejiang", "Zhejiang"],
  "安徽省": ["Anhui", "Anhui"], "福建省": ["Fujian", "Fujian"], "江西省": ["Jiangxi", "Jiangxi"], "山东省": ["Shandong", "Shandong"],
  "河南省": ["Henan", "Henan"], "湖北省": ["Hubei", "Hubei"], "湖南省": ["Hunan", "Hunan"], "广东省": ["Guangdong", "Guangdong"],
  "广西壮族自治区": ["Guangxi", "Guangxi"], "四川省": ["Sichuan", "Sichuan"], "贵州省": ["Guizhou", "Guizhou"], "云南省": ["Yunnan", "Yunnan"],
  "陕西省": ["Shaanxi", "Shaanxi"], "甘肃省": ["Gansu", "Gansu"], "青海省": ["Qinghai", "Qinghai"], "台湾省": ["Taiwan", "Taïwan"],
  "海南省": ["Hainan", "Hainan"], "宁夏回族自治区": ["Ningxia", "Ningxia"], "西藏自治区": ["Tibet", "Tibet"], "新疆维吾尔自治区": ["Xinjiang", "Xinjiang"],
  "香港特别行政区": ["Hong Kong", "Hong Kong"], "澳门特别行政区": ["Macau", "Macao"]
};

export function displayText(value, locale = "zh", kind = "name") {
  if (typeof value !== "string" || locale === "zh") return value;
  if (kind === "address") {
    const compactAddress = value.replace(/\//g, "");
    if (shopAddresses[compactAddress]) return shopAddresses[compactAddress][locale === "fr" ? 1 : 0];
    const replacements = locale === "fr"
      ? [["北京市", "Pékin, "], ["市辖区", ""], ["朝阳区", "district de Chaoyang, "], ["丰台区", "district de Fengtai, "], ["海淀区", "district de Haidian, "], ["东城区", "district de Dongcheng, "], ["西城区", "district de Xicheng, "], ["新街大道", "avenue Xinjie"], [/([0-9]+)号楼/g, "bâtiment $1"], [/([0-9]+)号/g, "n° $1"], [/([0-9]+)层/g, "$1e étage"]]
      : [["北京市", "Beijing, "], ["市辖区", ""], ["朝阳区", "Chaoyang District, "], ["丰台区", "Fengtai District, "], ["海淀区", "Haidian District, "], ["东城区", "Dongcheng District, "], ["西城区", "Xicheng District, "], ["新街大道", "Xinjie Avenue"], [/([0-9]+)号楼/g, "Building $1"], [/([0-9]+)号/g, "No. $1"], [/([0-9]+)层/g, "Floor $1"]];
    return replacements.reduce((address, [source, target]) => address.replace(source, target), compactAddress).replace(/,\s*,/g, ", ").trim();
  }
  if (kind === "region") {
    if (regions[value]) return regions[value][locale === "fr" ? 1 : 0];
    return value;
  }
  if (kind === "flavor") {
    return value.split(/[，,、]/).map((part) => {
      const text = part.trim();
      const translation = flavorNames[text];
      return translation ? translation[locale === "fr" ? 1 : 0] : text;
    }).join(", ");
  }
  const dictionary = kind === "category" ? categories : kind === "description" ? descriptions : menuNames;
  const translation = dictionary[value];
  if (translation) return translation[locale === "fr" ? 1 : 0];
  return value;
}
