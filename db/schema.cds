namespace sap.capire.bookshop;

using { Currency, managed, sap.common.CodeList } from '@sap/cds/common';

entity Books : managed {
  key ID       : Integer;
  title        : localized String(111)  @mandatory;
  descr        : localized String(1111);
  author       : Association to Authors  @mandatory;
  genre        : Association to Genres;
  stock        : Integer;
  price        : Decimal;
  currency     : Currency;
}

entity Authors : managed {
  key ID       : Integer;
  name         : String(111)  @mandatory;
  dateOfBirth  : Date;
  dateOfDeath  : Date;
  placeOfBirth : String;
  placeOfDeath : String;
  books        : Association to many Books on books.author = $self;
}

entity Genres : CodeList {
  key ID   : Integer;
  parent   : Association to Genres;
  children : Composition of many Genres on children.parent = $self;
}
