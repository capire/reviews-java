
export default {
  connect: {
    to (service, options) {
      if (!service.startsWith('/')) service = '/' + service
      if (!service.endsWith('/')) service = service + '/'
      return new RemoteService (service, options)
    }
  }
}

class RemoteService {

  constructor (service) { this.service = service }
  get GET() { return this.get.bind(this) }
  get PUT() { return this.put.bind(this) }
  get POST() { return this.post.bind(this) }
  get PATCH() { return this.patch.bind(this) }
  get DELETE() { return this.delete.bind(this) }

  async options (...args) { return axios.options(...this.args(...args)) }
  async head (...args) { return axios.head(...this.args(...args)) }
  async get (...args) { return axios.get(...this.args(...args)) }
  async put (...args) { return axios.put(...this.args(...args)) }
  async post (...args) { return axios.post(...this.args(...args)) }
  async patch (...args) { return axios.patch(...this.args(...args)) }
  async delete (...args) { return axios.delete(...this.args(...args)) }

  async list (...args) { return axios.get(...this.args(...args)) }
  async read (...args) { return axios.get(...this.args(...args)) }
  async create (...args) { return axios.post(...this.args(...args)) }
  async insert (...args) { return axios.post(...this.args(...args)) }
  async upsert (...args) { return axios.put(...this.args(...args)) }
  async update (...args) { return axios.patch(...this.args(...args)) }

  args (url, ...args) {
    if (url.raw) return [ this.service + String.raw(url,...args) ]
    else return [ this.service + url, ...args ]
  }
}


import axios from 'axios'

axios.interceptors.request.use(csrfToken)
function csrfToken (request) {
  if (request.method === 'head' || request.method === 'get') return request
  if ('csrfToken' in document) {
    request.headers['x-csrf-token'] = document.csrfToken
    return request
  }
  return fetchToken().then(token => {
    document.csrfToken = token
    request.headers['x-csrf-token'] = document.csrfToken
    return request
  }).catch(() => {
    document.csrfToken = null // set mark to not try again
    return request
  })

  function fetchToken() {
    return axios.get('/', { headers: { 'x-csrf-token': 'fetch' } })
    .then(res => res.headers['x-csrf-token'])
  }
}
