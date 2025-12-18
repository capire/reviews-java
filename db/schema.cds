namespace sap.capire.reviews;
using { User, managed } from '@sap/cds/common';

// Reviewed subjects can be any entity that is uniquely identified
// by a single key element, including UUIDs
type ReviewedSubject : String(111);

entity Reviews {
  key subject  : ReviewedSubject;
  key reviewer : User @cds.on.insert: $user;
  rating       : Rating @mandatory;
  title        : String(111);
  text         : String(1111);
  date         : managed:createdAt;
  likes        : Composition of many Likes on likes.review = $self;
  liked        : Integer default 0; // counter for likes as helpful review (count of all _likes belonging to this review)
}

type Rating : Integer @assert.range enum {
  Best  = 5;
  Good  = 4;
  Avg   = 3;
  Poor  = 2;
  Worst = 1;
}

entity Likes {
  key review : Association to Reviews;
  key user   : User;
}
