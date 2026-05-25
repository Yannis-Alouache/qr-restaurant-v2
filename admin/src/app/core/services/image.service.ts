import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ImageUploadResponse {
  url: string;
}

@Injectable({ providedIn: 'root' })
export class ImageService {
  private http = inject(HttpClient);

  upload(bucket: string, file: File): Observable<ImageUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ImageUploadResponse>(`/api/admin/images/${bucket}`, formData);
  }
}
