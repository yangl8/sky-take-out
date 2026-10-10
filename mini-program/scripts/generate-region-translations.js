const fs = require('fs');
const path = require('path');
const { pinyin } = require('pinyin-pro');

const root = path.resolve(__dirname, '..');
const dataRoot = path.join(root, 'pages/common/simple-address/city-data');
const output = path.join(root, 'utils/regionTranslations.json');

function readData(fileName, variableName) {
  const source = fs.readFileSync(path.join(dataRoot, fileName), 'utf8')
    .replace(/export default\s+\w+\s*;?\s*$/, '');
  return new Function(`${source}\nreturn ${variableName};`)();
}

const provinceData = readData('province.js', 'provinceData');
const cityData = readData('city.js', 'cityData');
const areaData = readData('area.js', 'areaData');

const pronunciationOverrides = {
  '长治': 'chang zhi', '襄垣': 'xiang yuan', '重庆': 'chong qing', '乐清': 'yue qing',
  '六安': "lu'an", '单': 'shan', '蚌埠': 'beng bu', '亳': 'bo',
  '厦门': 'xia men', '丽水': 'li shui', '台州': 'tai zhou', '铅山': 'yan shan',
  '番禺': 'pan yu', '盱眙': 'xu yi', '邛崃': 'qiong lai', '牟平': 'mu ping',
  '浚县': 'xun xian', '蔚县': 'yu xian', '涪陵': 'fu ling', '郫都': 'pi du',
  '酉阳': 'you yang', '筠连': 'jun lian', '犍为': 'qian wei', '茌平': 'chi ping',
  '莒县': 'ju xian', '郯城': 'tan cheng', '沭阳': 'shu yang', '繁峙': 'fan shi',
  '泌阳': 'bi yang', '栾城': 'luan cheng'
};

const ethnicNames = {
  '柯尔克孜族': ['Kyrgyz', 'kirghize'], '哈萨克族': ['Kazakh', 'kazakh'], '蒙古族': ['Mongolian', 'mongol'],
  '朝鲜族': ['Korean', 'coréen'], '俄罗斯族': ['Russian', 'russe'], '鄂温克族': ['Evenki', 'évène'],
  '东乡族': ['Dongxiang', 'dongxiang'], '撒拉族': ['Salar', 'salar'], '保安族': ['Bonan', 'bonan'],
  '裕固族': ['Yugur', 'yugur'], '傈僳族': ['Lisu', 'lisu'], '哈尼族': ['Hani', 'hani'],
  '布依族': ['Bouyei', 'bouyei'], '土家族': ['Tujia', 'tujia'], '藏族': ['Tibetan', 'tibétain'],
  '羌族': ['Qiang', 'qiang'], '彝族': ['Yi', 'yi'], '回族': ['Hui', 'hui'], '壮族': ['Zhuang', 'zhuang'],
  '苗族': ['Miao', 'miao'], '侗族': ['Dong', 'dong'], '白族': ['Bai', 'bai'], '傣族': ['Dai', 'dai'],
  '纳西族': ['Naxi', 'naxi'], '佤族': ['Wa', 'wa'], '瑶族': ['Yao', 'yao'], '满族': ['Manchu', 'mandchou'],
  '畲族': ['She', 'she'], '水族': ['Shui', 'shui'], '仫佬族': ['Mulao', 'mulao'], '毛南族': ['Maonan', 'maonan'],
  '普米族': ['Pumi', 'pumi'], '阿昌族': ['Achang', 'achang'], '怒族': ['Nu', 'nu'], '德昂族': ['Deang', 'deang'],
  '基诺族': ['Jino', 'jino'], '独龙族': ['Dulong', 'dulong'], '塔吉克族': ['Tajik', 'tadjik'],
  '乌孜别克族': ['Uzbek', 'ouzbek'], '塔塔尔族': ['Tatar', 'tatar'], '景颇族': ['Jingpo', 'jingpo']
};

function romanize(value) {
  const override = pronunciationOverrides[value];
  const syllables = override || pinyin(value, { toneType: 'none' });
  const word = syllables.replace(/\s+/g, '').toLowerCase();
  return word ? word[0].toUpperCase() + word.slice(1) : '';
}

function translate(label, level) {
  if (label === '市辖区') return ['Urban District', 'district urbain'];
  if (label === '省直辖县级行政区划' || label === '自治区直辖县级行政区划') {
    return ['Directly Administered County-level Divisions', 'divisions de niveau xian directement administrées'];
  }

  const endings = level === 'province'
    ? [['特别行政区', 'Special Administrative Region', 'région administrative spéciale'], ['自治区', 'Autonomous Region', 'région autonome'], ['省', 'Province', 'province'], ['市', 'Municipality', 'municipalité']]
    : level === 'city'
      ? [['自治州', 'Autonomous Prefecture', 'préfecture autonome'], ['地区', 'Prefecture', 'préfecture'], ['盟', 'League', 'ligue'], ['市', 'City', 'ville']]
      : [['自治县', 'Autonomous County', 'xian autonome'], ['自治旗', 'Autonomous Banner', 'bannière autonome'], ['自治州', 'Autonomous Prefecture', 'préfecture autonome'], ['林区', 'Forest District', 'district forestier'], ['特区', 'Special District', 'district spécial'], ['开发区', 'Development Zone', 'zone de développement'], ['新区', 'New District', 'nouveau district'], ['工业园区', 'Industrial Park', 'parc industriel'], ['管理区', 'Management District', 'district administratif'], ['旗', 'Banner', 'bannière'], ['县', 'County', 'xian'], ['区', 'District', 'district'], ['市', 'City', 'ville']];

  let stem = label;
  let suffix = null;
  for (const ending of endings) {
    if (label.endsWith(ending[0])) {
      stem = label.slice(0, -ending[0].length);
      suffix = ending;
      break;
    }
  }
  if (!suffix) suffix = ['', '', ''];

  const ethnic = Object.keys(ethnicNames).sort((a, b) => b.length - a.length);
  let englishStem = '';
  let frenchStem = '';
  let remaining = stem;
  for (const group of ethnic) {
    if (!remaining.includes(group)) continue;
    const [english, french] = ethnicNames[group];
    const before = remaining.slice(0, remaining.indexOf(group));
    englishStem += before ? `${romanize(before)} ` : '';
    frenchStem += before ? `${romanize(before)} ` : '';
    englishStem += `${englishStem ? ' ' : ''}${english}`;
    frenchStem += `${frenchStem ? ' ' : ''}${french}`;
    remaining = remaining.slice(remaining.indexOf(group) + group.length);
  }
  if (remaining) {
    const extra = romanize(remaining);
    englishStem += `${englishStem ? ' ' : ''}${extra}`;
    frenchStem += `${frenchStem ? ' ' : ''}${extra}`;
  }
  englishStem = englishStem.replace(/\s+/g, ' ').trim();
  frenchStem = frenchStem.replace(/\s+/g, ' ').trim();

  if (!stem) return [suffix[1], suffix[2]];
  if (level === 'province') {
    return [`${englishStem} ${suffix[1]}`.trim(), `${suffix[2]} de ${frenchStem}`.trim()];
  }
  return [`${englishStem} ${suffix[1]}`.trim(), `${suffix[2]} de ${frenchStem}`.trim()];
}

const records = {};
const codeLabels = {};
const names = new Set();
const namesMap = {};
let repeatedCodeCount = 0;
const add = (code, item, level) => {
  if (!/^\d{6}$/.test(code)) throw new Error(`Invalid six-digit division code: ${code}`);
  if (!item || typeof item.label !== 'string' || !item.label.trim()) throw new Error(`Empty name for ${code}`);
  if (records[code]) {
    const translation = translate(item.label, level);
    if (codeLabels[code] !== item.label || JSON.stringify(records[code].slice(1)) !== JSON.stringify(translation)) {
      throw new Error(`Conflicting division data for code ${code}: ${codeLabels[code]} / ${item.label}`);
    }
    repeatedCodeCount += 1;
    return;
  }
  names.add(item.label);
  const translatedName = translate(item.label, level);
  if (namesMap[item.label] && JSON.stringify(namesMap[item.label]) !== JSON.stringify(translatedName)) {
    throw new Error(`Conflicting translations for repeated name ${item.label}`);
  }
  namesMap[item.label] = translatedName;
  codeLabels[code] = item.label;
  records[code] = [item.label, ...translatedName];
};

provinceData.forEach((province, provinceIndex) => {
  add(`${province.value}0000`, province, 'province');
  (cityData[provinceIndex] || []).forEach((city, cityIndex) => {
    add(`${city.value}00`, city, 'city');
    (((areaData[provinceIndex] || [])[cityIndex]) || []).forEach((area) => add(area.value, area, 'county'));
  });
});

fs.writeFileSync(output, `${JSON.stringify(records)}\n`);
console.log(`Wrote ${Object.keys(records).length} coded translations for ${names.size} distinct labels; reused ${repeatedCodeCount} repeated identical codes in the source hierarchy`);
