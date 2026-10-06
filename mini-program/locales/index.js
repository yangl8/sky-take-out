import zh from "./zh";
import fr from "./fr";
import en from "./en";

const LOCALE_STORAGE_KEY = "locale";

const messages = {
  zh,
  fr,
  en,
};

export const supportedLocales = [
  { code: "fr", labelKey: "common.french" },
  { code: "en", labelKey: "common.english" },
  { code: "zh", labelKey: "common.chinese" },
];

export function getInitialLocale() {
  const savedLocale = uni.getStorageSync(LOCALE_STORAGE_KEY);
  if (messages[savedLocale]) {
    return savedLocale;
  }

  const systemLanguage = uni.getSystemInfoSync().language || "";
  const language = systemLanguage.toLowerCase();
  if (language.startsWith("fr")) {
    return "fr";
  }
  if (language.startsWith("en")) {
    return "en";
  }
  return "zh";
}

export function saveLocale(locale) {
  if (messages[locale]) {
    uni.setStorageSync(LOCALE_STORAGE_KEY, locale);
  }
}

export function translate(locale, key, params = {}) {
  const keys = key.split(".");
  const value = keys.reduce((result, currentKey) => {
    return result && result[currentKey];
  }, messages[locale]);

  const message = value || keys.reduce((result, currentKey) => {
    return result && result[currentKey];
  }, messages.zh) || key;

  return Object.keys(params).reduce((result, param) => {
    return result.replace(new RegExp(`\\{${param}\\}`, "g"), params[param]);
  }, message);
}
