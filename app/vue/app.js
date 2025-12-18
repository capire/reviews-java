import { createApp, ref, reactive } from 'vue'
import cds from './cap.js'

const { GET, POST, PUT } = await cds.connect.to ('/odata/v4/sap.capire.reviews.app.ReviewsService/')
createApp ({ setup() {

  const $ = sel => document.querySelector(sel)
  const reviews = ref([])
  const review = ref({
    /** The reviewed subject, e.g. a book */  subject: '',
    /** The reviewer */                       reviewer: '',
    /** The review text */                    text: '',
    /** The review rating */                  rating: 0,
  })

  const message = reactive({
    succeeded: undefined,
    failed: undefined,
    reset() { this.succeeded = this.failed = undefined }
  })

  return {
    reviews, review, message, Ratings: {
      5: '★★★★★',
      4: '★★★★☆',
      3: '★★★☆☆',
      2: '★★☆☆☆',
      1: '★☆☆☆☆',
    },
    at: date => date && new Date(date).toDateString(),

    async fetch (pattern) {
      const { data } = await GET `ListOfReviews${pattern ? `?$search=${pattern}` : ''}`
      reviews.value = data.value
      review.value = undefined
      $('input#search').focus()
    },

    async edit (index) {
      if (index !== undefined) {
        review.value = reviews.value [index]
        const { subject, reviewer } = review.value
        const { data } = await GET `Reviews(subject='${subject}',reviewer='${reviewer}')/text/$value`
        review.value.text = data
      } else {
        review.value = {}
        setTimeout (()=> $('form > input').focus(), 111)
      }
      message.reset()
    },

    like: ()=> {
      message.reset()
      const { subject, reviewer } = review.value
      POST (`like`, {review:{subject,reviewer}})
        .then (()=> { message.succeeded = 'Your like was submitted. Thanks.'})
        .catch (e => message.failed = e.response.data.error.message)
    },

    unlike: ()=> {
      message.reset()
      const { subject, reviewer } = review.value
      POST (`unlike`, {review:{subject,reviewer}})
        .then (()=> { message.succeeded = 'Your unlike was submitted. Thanks.'})
        .catch (e => message.failed = e.response.data.error.message)
    },

    submit: ()=> {
      message.reset()
      review.value.rating = parseInt(review.value.rating)
      const { subject, reviewer } = review.value
      if(reviewer === undefined)
        POST (`Reviews`, review.value)
        .then ((result)=> { review.value = result.data; message.succeeded = 'Your review was submitted. Thanks.'})
        .catch (e => message.failed = e.response.data.error.message)
      else
        PUT (`Reviews(subject='${subject}',reviewer='${reviewer}')`, review.value)
        .then (()=> message.succeeded = 'Your review was changed. Thanks.')
        .catch (e => message.failed = e.response.data.error.message)
    }
  }

}}) .mount("#app") .fetch()
