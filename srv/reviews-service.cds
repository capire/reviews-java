using { sap.capire.reviews as my } from '../db/schema';
namespace sap.capire.reviews.app;

type Review {
  subject  : my.Reviews:subject;
  reviewer : my.Reviews:reviewer;
}

@(requires: 'any')
service ReviewsService {

  entity ListOfReviews as projection on my.Reviews excluding { text, likes };
  entity Reviews @cds.redirection.target as projection on my.Reviews;

  // Actions for liking and unliking reviews
  entity Likes as projection on my.Likes;
  action like (review: Review);
  action unlike (review: Review);

}


// Access control restrictions
annotate ReviewsService.Reviews with @restrict:[
  { grant:'READ',   to:'any' },                 // everybody can read reviews
  { grant:'CREATE', to:'authenticated-user' },  // users must login to add reviews
  { grant:'UPDATE', to:'authenticated-user', where:'reviewer=$user' },
  { grant:'DELETE', to:'admin' },
];

annotate ReviewsService with @restrict:[
  { grant:'like', to:'identified-user' },
  { grant:'unlike', to:'identified-user', where:'user=$user' },
];
