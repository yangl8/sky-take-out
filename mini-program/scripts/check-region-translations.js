const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const dataRoot = path.join(root, 'pages/common/simple-address/city-data');
const translations = require('../utils/regionTranslations.json');
const { getDemoDeliveryAddress } = require('../utils/demoDeliveryAddress.js');

function readData(fileName, variableName) {
  const source = fs.readFileSync(path.join(dataRoot, fileName), 'utf8')
    .replace(/export default\s+\w+\s*;?\s*$/, '');
  return new Function(`${source}\nreturn ${variableName};`)();
}

const provinceData = readData('province.js', 'provinceData');
const cityData = readData('city.js', 'cityData');
const areaData = readData('area.js', 'areaData');
const source = fs.readFileSync(path.join(root, 'utils/displayText.js'), 'utf8')
  .replace(/^import regionTranslations from .*;\s*$/m, '')
  .replace(/^const \{ getDemoDeliveryAddress \} = require\(.*;\s*$/m, '')
  .replace('export function displayText', 'function displayText');
const displayText = new Function('regionTranslations', 'getDemoDeliveryAddress', `${source}\nreturn displayText;`)(translations, getDemoDeliveryAddress);
const codes = new Map();
const labels = new Set();
const errors = [];
let duplicateCodes = 0;

const demoAddress = '北京市市辖区朝阳区三里屯路19号';
if (displayText(demoAddress, 'zh', 'address') !== demoAddress) errors.push('demo address changed in Chinese');
if (displayText(demoAddress, 'en', 'address') !== '19 Sanlitun Road, Chaoyang District, Beijing') errors.push('demo address English translation mismatch');
if (displayText(demoAddress, 'fr', 'address') !== '19 rue Sanlitun, district de Chaoyang, Pékin') errors.push('demo address French translation mismatch');
if (displayText('北京市 / 市辖区 / 朝阳区 / 三里屯路19号', 'en', 'address') !== '19 Sanlitun Road, Chaoyang District, Beijing') errors.push('demo address separator normalization mismatch');
if (displayText('北京市市辖区朝阳区三里屯路20号', 'en', 'address') !== 'Beijing, Chaoyang District, 三里屯路20号') errors.push('different door number or user-entered detail was modified');
if (displayText('北京市市辖区朝阳区新街大道1号', 'fr', 'address') !== 'Pékin, district de Chaoyang, 新街大道1号') errors.push('non-demo user detail was modified');

function checkEntry(code, item) {
  if (!/^\d{6}$/.test(code)) errors.push(`invalid code ${code}`);
  if (!item || !item.label || !item.label.trim()) {
    errors.push(`empty label for ${code}`);
    return;
  }
  labels.add(item.label);
  const previous = codes.get(code);
  if (previous) {
    if (previous !== item.label) errors.push(`code ${code} maps to both ${previous} and ${item.label}`);
    else duplicateCodes += 1;
  } else {
    codes.set(code, item.label);
  }
  const pair = translations[code];
  if (!Array.isArray(pair) || pair.length !== 3 || pair[0] !== item.label || pair.slice(1).some((text) => !text || /[\u3400-\u9fff]/.test(text))) {
    errors.push(`missing or invalid bilingual translations for ${code} (${item.label})`);
  }
  for (const locale of ['en', 'fr']) {
    const shown = displayText(item.label, locale, 'region', code);
    if (!shown || shown === item.label || /[\u3400-\u9fff]/.test(shown)) errors.push(`${locale} display failed for ${code} (${item.label}): ${shown}`);
  }
  if (displayText(item.label, 'zh', 'region', code) !== item.label) errors.push(`Chinese display changed for ${code}`);
}

provinceData.forEach((province, provinceIndex) => {
  checkEntry(`${province.value}0000`, province);
  (cityData[provinceIndex] || []).forEach((city, cityIndex) => {
    checkEntry(`${city.value}00`, city);
    (((areaData[provinceIndex] || [])[cityIndex]) || []).forEach((area) => checkEntry(area.value, area));
  });
});

if (Object.keys(translations).length !== codes.size) errors.push(`translation code count ${Object.keys(translations).length} differs from source unique code count ${codes.size}`);
const translatedLabels = new Set(Object.values(translations).map(([label]) => label));
if (translatedLabels.size !== labels.size) errors.push(`translated label count ${translatedLabels.size} differs from source unique label count ${labels.size}`);

const sample = (label, locale, expected) => {
  const entry = Object.entries(translations).find(([code]) => codes.get(code) === label);
  const shown = entry && displayText(label, locale, 'region', entry[0]);
  if (!shown || !shown.includes(expected)) errors.push(`expected ${label} in ${locale} to include ${expected}; received ${shown}`);
};
sample('北京市', 'en', 'Beijing');
sample('上海市', 'fr', 'Shanghai');
sample('重庆市', 'en', 'Chongqing');
sample('长治市', 'en', 'Changzhi');
sample('襄垣县', 'fr', 'Xiangyuan');
sample('乐清市', 'en', 'Yueqing');
sample('单县', 'en', 'Shan County');
sample('六安市', 'en', "Lu'an");
sample('阿坝藏族羌族自治州', 'en', 'Tibetan Qiang Autonomous Prefecture');
sample('长白朝鲜族自治县', 'en', 'Korean Autonomous County');
sample('察哈尔右翼前旗', 'en', 'Banner');
const autonomousPrefecture = [...codes.entries()].find(([, label]) => label.endsWith('自治州'));
if (!autonomousPrefecture || !displayText(autonomousPrefecture[1], 'en', 'region', autonomousPrefecture[0]).includes('Autonomous Prefecture')) {
  errors.push('autonomous prefecture suffix check failed');
}
const banner = [...codes.entries()].find(([, label]) => label.endsWith('旗'));
if (!banner || !displayText(banner[1], 'fr', 'region', banner[0]).includes('bannière')) errors.push('banner suffix check failed');
const englishAddress = displayText('山西省长治市襄垣县2', 'en', 'address');
const frenchAddress = displayText('山西省长治市襄垣县2', 'fr', 'address');
if (!englishAddress.includes('Xiangyuan County') || !frenchAddress.includes('district de Xiangyuan')) errors.push('translated address display check failed');
if (displayText('山西省长治市襄垣县2', 'zh', 'address') !== '山西省长治市襄垣县2') errors.push('Chinese address display changed');

const selectorSource = fs.readFileSync(path.join(root, 'pages/common/simple-address/simple-address.nvue'), 'utf8');
const addAddressSource = fs.readFileSync(path.join(root, 'pages/addOrEditAddress/addOrEditAddress.vue'), 'utf8');
if ((selectorSource.match(/\$displayText\(item\.label, 'region', item\.value/g) || []).length !== 6) errors.push('picker does not pass region codes for every platform column');
if (!selectorSource.includes('label: this._getLabel()') || !selectorSource.includes('return pcikerLabel;')) errors.push('picker no longer emits its original Chinese labels');
if (!addAddressSource.includes('this.address = e.label') || !addAddressSource.includes('provinceName: this.address.split("/")[0]')) errors.push('address save path no longer uses raw picker labels');
if (/pinyin-pro/.test(fs.readFileSync(path.join(root, 'utils/displayText.js'), 'utf8'))) errors.push('runtime display utility depends on the generator package');

if (errors.length) {
  console.error(errors.join('\n'));
  process.exit(1);
}
console.log(`Validated ${codes.size} unique codes and ${labels.size} distinct region names in English, French, and Chinese; ${duplicateCodes} repeated source codes have identical labels; picker save path keeps the original Chinese labels.`);
