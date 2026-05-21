using { sap.capire.bookshop as my } from '../db/schema';

service CatalogService @(path:'/odata/v4/browse') {

  @readonly entity Books   as projection on my.Books;
  @readonly entity Authors as projection on my.Authors;

  @requires: 'authenticated-user'
  action submitOrder ( book: Books:ID, quantity: Integer ) returns { stock: Integer };
}

service AdminService @(path:'/odata/v4/admin', requires: 'authenticated-user') {
  entity Books   as projection on my.Books;
  entity Authors as projection on my.Authors;
}
