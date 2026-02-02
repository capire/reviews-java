using ReviewsService from './reviews-service';

// Access control restrictions on entity Reviews
annotate ReviewsService.Reviews with @restrict:[
  { grant:'READ',   to:'any' },                 // everybody can read reviews
  { grant:'CREATE', to:'authenticated-user' },  // users must login to add reviews
  { grant:'UPDATE', to:'authenticated-user', where:'reviewer=$user' },
  { grant:'DELETE', to:'admin' },
];

// Access control restrictions on service-level actions
annotate ReviewsService with @restrict:[
  { grant:'like', to:'identified-user' },
  { grant:'unlike', to:'identified-user', where:'user=$user' },
];
