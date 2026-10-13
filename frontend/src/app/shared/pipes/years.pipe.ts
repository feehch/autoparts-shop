import { Pipe, PipeTransform } from '@angular/core';
import { CarModelDto } from '../../core/models/api.models';

/** Годы выпуска модели: «2017 — н.в.». */
@Pipe({ name: 'years' })
export class YearsPipe implements PipeTransform {
  transform(m: Pick<CarModelDto, 'yearFrom' | 'yearTo'> | null | undefined): string {
    if (!m || (m.yearFrom == null && m.yearTo == null)) return '—';
    return `${m.yearFrom ?? '…'} — ${m.yearTo ?? 'н.в.'}`;
  }
}
