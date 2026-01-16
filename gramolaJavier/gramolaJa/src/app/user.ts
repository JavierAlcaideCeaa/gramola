import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LoginResponse {
  email: string;
  barName: string;
  clientId: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);
  private apiUrl = 'http://127.0.0.1:8080/user';  // ✅ CAMBIAR

  register(email: string, pwd1: string, pwd2: string, barName: string, clientId: string, clientSecret: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/register`, {
      email,
      pwd1,
      pwd2,
      barName,
      clientId,
      clientSecret
    });
  }
  
  login(email: string, pwd: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, 
      { email, pwd },
      { withCredentials: true }
    );
  }
}