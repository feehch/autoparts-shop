import { HttpParams } from '@angular/common/http';

/** Формирует HttpParams, отбрасывая пустые значения (null, undefined, '', false для флагов). */
export function toHttpParams(obj: object): HttpParams {
  let params = new HttpParams();
  for (const [key, value] of Object.entries(obj)) {
    if (value === null || value === undefined || value === '' || value === false) continue;
    params = params.set(key, String(value));
  }
  return params;
}
