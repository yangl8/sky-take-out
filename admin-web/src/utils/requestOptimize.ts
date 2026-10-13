import md5 from 'md5';

const getRequestKey = (config) => {
    if (!config) {
        return md5(+new Date());
    }

    const data = typeof config.data === 'string' ? config.data : JSON.stringify(config.data);
    // console.log(config,pending,config.url,md5(config.url + '&' + config.method + '&' + data),'config')
    return md5(config.url + '&' + config.method + '&' + data);
}

const pending = {};
const checkPending = (key) => !!pending[key];
const removePending = (key) => {
    // console.log(key,'key')
    delete pending[key];
};

export {
    getRequestKey,
    pending,
    checkPending,
    removePending
}
